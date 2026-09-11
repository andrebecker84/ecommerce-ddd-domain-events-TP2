package br.edu.infnet.ecommerce.pagamento;

import br.edu.infnet.ecommerce.pagamento.application.PagamentoAppService;
import br.edu.infnet.ecommerce.pagamento.domain.Dinheiro;
import br.edu.infnet.ecommerce.pagamento.domain.MotivoRecusa;
import br.edu.infnet.ecommerce.pagamento.domain.NumeroCartao;
import br.edu.infnet.ecommerce.pagamento.domain.Pagamento;
import br.edu.infnet.ecommerce.pagamento.domain.PagamentoId;
import br.edu.infnet.ecommerce.pagamento.domain.PagamentoRepositorio;
import br.edu.infnet.ecommerce.pagamento.domain.ProcessadorCartao;
import br.edu.infnet.ecommerce.pagamento.domain.ResultadoAutorizacao;
import br.edu.infnet.ecommerce.pagamento.domain.StatusPagamento;
import br.edu.infnet.ecommerce.pagamento.domain.evento.EventoDeDominio;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PagamentoAprovado;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PagamentoRecusado;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PublicadorDeEventos;
import br.edu.infnet.ecommerce.pedido.pagamento.ResultadoPagamento;
import br.edu.infnet.ecommerce.pedido.pagamento.SolicitacaoPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * As cinco regras do enunciado, verificadas sem subir contexto Spring.
 *
 * O domínio não depende de framework, então os testes rodam em memória com
 * dublês simples, o que só é possível porque o repositório e o processador
 * são portas, e não classes concretas.
 */
class PagamentoAppServiceTest {

    /** Ordem em que repositório e publicador foram acionados, na mesma execução. */
    private final List<String> chamadas = new ArrayList<>();

    private RepositorioEmMemoria repositorio;
    private PublicadorEmMemoria publicador;
    private PagamentoAppService servico;

    @BeforeEach
    void preparar() {
        chamadas.clear();
        repositorio = new RepositorioEmMemoria();
        publicador = new PublicadorEmMemoria();
        servico = new PagamentoAppService(repositorio, new ProcessadorCartaoFake(), publicador);
    }

    @Test
    @DisplayName("valor menor ou igual a zero: recusado")
    void valorZeroOuNegativoRecusado() {
        ResultadoPagamento zero = servico.processar(solicitacao("0.00", "4111111111111111"));
        assertEquals("RECUSADO", zero.status());
        assertEquals(MotivoRecusa.VALOR_INVALIDO.name(), zero.motivo());

        ResultadoPagamento negativo = servico.processar(solicitacao("-10.00", "4111111111111111"));
        assertEquals("RECUSADO", negativo.status());
        assertEquals(MotivoRecusa.VALOR_INVALIDO.name(), negativo.motivo());
    }

    @Test
    @DisplayName("valor acima de R$ 10.000,00: recusado por limite")
    void acimaDoLimiteRecusado() {
        ResultadoPagamento resultado =
                servico.processar(solicitacao("10000.01", "4111111111111111"));

        assertEquals("RECUSADO", resultado.status());
        assertEquals(MotivoRecusa.LIMITE_EXCEDIDO.name(), resultado.motivo());
    }

    @Test
    @DisplayName("exatamente R$ 10.000,00 continua aprovado")
    void limiteExatoAprovado() {
        ResultadoPagamento resultado =
                servico.processar(solicitacao("10000.00", "4111111111111111"));

        assertEquals("APROVADO", resultado.status());
    }

    @Test
    @DisplayName("cartão terminado em 0000: bloqueado")
    void cartaoBloqueado() {
        ResultadoPagamento resultado =
                servico.processar(solicitacao("500.00", "4111111111110000"));

        assertEquals("RECUSADO", resultado.status());
        assertEquals(MotivoRecusa.CARTAO_BLOQUEADO.name(), resultado.motivo());
        assertNull(resultado.codigoAutorizacao());
    }

    @Test
    @DisplayName("cartão terminado em 1111: aprovado")
    void cartaoTerminadoEm1111Aprovado() {
        ResultadoPagamento resultado =
                servico.processar(solicitacao("500.00", "4111111111111111"));

        assertEquals("APROVADO", resultado.status());
        assertNotNull(resultado.codigoAutorizacao());
        assertNull(resultado.motivo());
    }

    @Test
    @DisplayName("demais cartões válidos: aprovados")
    void demaisCartoesAprovados() {
        ResultadoPagamento resultado =
                servico.processar(solicitacao("500.00", "5555555555554444"));

        assertEquals("APROVADO", resultado.status());
        assertNotNull(resultado.codigoAutorizacao());
    }

    @Test
    @DisplayName("forma de pagamento não suportada: recusada como regra de negócio")
    void formaNaoSuportadaRecusada() {
        ResultadoPagamento resultado = servico.processar(new SolicitacaoPagamento(
                1L, 1L, new BigDecimal("500.00"), "PIX", "4111111111111111"));

        assertEquals("RECUSADO", resultado.status());
        assertEquals(MotivoRecusa.FORMA_PAGAMENTO_NAO_SUPORTADA.name(), resultado.motivo());
    }

    @Test
    @DisplayName("o pagamento processado é persistido pelo repositório do contexto")
    void pagamentoPersistido() {
        servico.processar(solicitacao("500.00", "4111111111111111"));

        assertEquals(1, repositorio.salvos.size());
        Pagamento persistido = repositorio.salvos.getFirst();
        assertEquals(StatusPagamento.APROVADO, persistido.status());
        assertEquals(1L, persistido.pedidoId());
        assertEquals("**** **** **** 1111", persistido.cartaoMascarado());
    }

    @Test
    @DisplayName("o agregado não aceita ser processado duas vezes")
    void naoProcessaDuasVezes() {
        Pagamento pagamento = Pagamento.solicitar(
                1L, 1L, Dinheiro.reais(new BigDecimal("100.00")),
                br.edu.infnet.ecommerce.pagamento.domain.FormaPagamento.CARTAO,
                new NumeroCartao("4111111111111111"));

        pagamento.aprovar("ABC12345");

        assertThrows(IllegalStateException.class, () -> pagamento.aprovar("OUTRO"));
        assertThrows(IllegalStateException.class,
                () -> pagamento.recusar(MotivoRecusa.CARTAO_BLOQUEADO));
    }

    @Test
    @DisplayName("o número completo do cartão nunca sai do Value Object")
    void numeroDoCartaoNaoVaza() {
        NumeroCartao cartao = new NumeroCartao("4111111111111111");

        assertEquals("**** **** **** 1111", cartao.toString());
        assertFalse(cartao.toString().contains("4111111111111111"));
        assertTrue(cartao.mascarado().endsWith("1111"));
    }

    @Test
    @DisplayName("cartão em formato inválido é rejeitado na criação do Value Object")
    void cartaoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new NumeroCartao("123"));
        assertThrows(IllegalArgumentException.class, () -> new NumeroCartao("abcd"));
        assertThrows(IllegalArgumentException.class, () -> new NumeroCartao(null));
    }

    @Test
    @DisplayName("cartão ilegível vira recusa de negócio, como no legado, e não erro técnico")
    void cartaoIlegivelVirarRecusa() {
        ResultadoPagamento resultado =
                servico.processar(solicitacao("500.00", "123"));

        assertEquals("RECUSADO", resultado.status());
        assertEquals(MotivoRecusa.CARTAO_INVALIDO.name(), resultado.motivo());
        assertNull(resultado.codigoAutorizacao());
    }

    @Test
    @DisplayName("o pagamento recusado por cartão ilegível também é registrado")
    void cartaoIlegivelPersistido() {
        servico.processar(solicitacao("500.00", "nao-e-cartao"));

        assertEquals(1, repositorio.salvos.size());
        Pagamento persistido = repositorio.salvos.getFirst();
        assertEquals(StatusPagamento.RECUSADO, persistido.status());
        assertEquals(MotivoRecusa.CARTAO_INVALIDO, persistido.motivo());
        assertEquals("****", persistido.cartaoMascarado());
    }

    @Test
    @DisplayName("NumeroCartao.tentar devolve vazio em vez de lançar")
    void tentarNaoLanca() {
        assertTrue(NumeroCartao.tentar("123").isEmpty());
        assertTrue(NumeroCartao.tentar(null).isEmpty());
        assertTrue(NumeroCartao.tentar("4111111111111111").isPresent());
    }

    @Test
    @DisplayName("pagamento aprovado publica PagamentoAprovado uma única vez")
    void aprovacaoPublicaEvento() {
        servico.processar(solicitacao("250.00", "4111111111111111"));

        assertEquals(1, publicador.publicados.size());
        PagamentoAprovado evento =
                assertInstanceOf(PagamentoAprovado.class, publicador.publicados.getFirst());
        assertEquals(new BigDecimal("250.00"), evento.valor());
        assertEquals(repositorio.salvos.getFirst().id().toString(), evento.agregadoId());
    }

    @Test
    @DisplayName("pagamento recusado publica PagamentoRecusado com o motivo")
    void recusaPublicaEvento() {
        servico.processar(solicitacao("500.00", "4111111111110000"));

        PagamentoRecusado evento =
                assertInstanceOf(PagamentoRecusado.class, publicador.publicados.getFirst());
        assertEquals(MotivoRecusa.CARTAO_BLOQUEADO.name(), evento.motivo());
    }

    @Test
    @DisplayName("o agregado é limpo depois da publicação, e o mesmo fato não sai duas vezes")
    void agregadoLimpoAposPublicar() {
        servico.processar(solicitacao("250.00", "4111111111111111"));

        assertTrue(repositorio.salvos.getFirst().eventosRegistrados().isEmpty());
    }

    @Test
    @DisplayName("a publicação acontece depois de persistir")
    void publicaDepoisDeSalvar() {
        servico.processar(solicitacao("250.00", "4111111111111111"));

        assertEquals(List.of("salvar", "publicar"), chamadas);
    }

    private SolicitacaoPagamento solicitacao(String valor, String cartao) {
        return new SolicitacaoPagamento(1L, 1L, new BigDecimal(valor), "CARTAO", cartao);
    }

    /**
     * Dublê da porta de publicação. Guarda os eventos recebidos e anota a chamada na
     * lista compartilhada com o repositório, e é assim que a ordem fica verificável.
     */
    private final class PublicadorEmMemoria implements PublicadorDeEventos {
        private final List<EventoDeDominio> publicados = new ArrayList<>();

        @Override
        public void publicar(List<EventoDeDominio> eventos) {
            chamadas.add("publicar");
            publicados.addAll(eventos);
        }
    }

    /** Dublê do processador com o mesmo comportamento do adaptador simulado. */
    private static final class ProcessadorCartaoFake implements ProcessadorCartao {
        @Override
        public ResultadoAutorizacao autorizar(NumeroCartao cartao, Dinheiro valor) {
            if ("0000".equals(cartao.ultimosQuatro())) {
                return ResultadoAutorizacao.negado(MotivoRecusa.CARTAO_BLOQUEADO);
            }
            return ResultadoAutorizacao.autorizado("AUTH" + UUID.randomUUID().toString()
                    .substring(0, 4).toUpperCase());
        }
    }

    private final class RepositorioEmMemoria implements PagamentoRepositorio {
        private final List<Pagamento> salvos = new ArrayList<>();

        @Override
        public void salvar(Pagamento pagamento) {
            chamadas.add("salvar");
            salvos.add(pagamento);
        }

        @Override
        public Optional<Pagamento> porId(PagamentoId id) {
            return salvos.stream().filter(p -> p.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Pagamento> porPedido(long pedidoId) {
            return salvos.stream().filter(p -> p.pedidoId() == pedidoId).findFirst();
        }
    }
}
