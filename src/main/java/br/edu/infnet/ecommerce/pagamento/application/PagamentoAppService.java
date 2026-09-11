package br.edu.infnet.ecommerce.pagamento.application;

import br.edu.infnet.ecommerce.pagamento.domain.Dinheiro;
import br.edu.infnet.ecommerce.pagamento.domain.FormaPagamento;
import br.edu.infnet.ecommerce.pagamento.domain.MotivoRecusa;
import br.edu.infnet.ecommerce.pagamento.domain.NumeroCartao;
import br.edu.infnet.ecommerce.pagamento.domain.Pagamento;
import br.edu.infnet.ecommerce.pagamento.domain.PagamentoRepositorio;
import br.edu.infnet.ecommerce.pagamento.domain.ProcessadorCartao;
import br.edu.infnet.ecommerce.pagamento.domain.ResultadoAutorizacao;
import br.edu.infnet.ecommerce.pagamento.domain.evento.EventoDeDominio;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PublicadorDeEventos;
import br.edu.infnet.ecommerce.pedido.pagamento.ResultadoPagamento;
import br.edu.infnet.ecommerce.pedido.pagamento.SolicitacaoPagamento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Serviço de aplicação do contexto de Pagamento.
 *
 * Não contém regra de negócio: traduz a entrada, delega a decisão ao agregado,
 * aciona a porta do processador quando a política interna permite e persiste o
 * resultado. Uma transação, um agregado.
 *
 * Este serviço não conhece UsuarioRepository, ProdutoRepository,
 * EstoqueRepository nem PedidoRepository, apenas os identificadores recebidos.
 */
@Service
public class PagamentoAppService {

    private final PagamentoRepositorio repositorio;
    private final ProcessadorCartao processador;
    private final PublicadorDeEventos publicador;

    public PagamentoAppService(PagamentoRepositorio repositorio,
                               ProcessadorCartao processador,
                               PublicadorDeEventos publicador) {
        this.repositorio = repositorio;
        this.processador = processador;
        this.publicador = publicador;
    }

    @Transactional
    public ResultadoPagamento processar(SolicitacaoPagamento solicitacao) {

        Dinheiro valor = Dinheiro.reais(
                solicitacao.valor() == null ? BigDecimal.ZERO : solicitacao.valor());
        FormaPagamento forma = FormaPagamento.reconhecer(solicitacao.formaPagamento());

        // Cartão ilegível é recusa de negócio, não erro técnico: o pagamento
        // é registrado como RECUSADO em vez de a requisição falhar.
        Optional<NumeroCartao> cartao = NumeroCartao.tentar(solicitacao.numeroCartao());
        if (cartao.isEmpty()) {
            Pagamento recusado = Pagamento.recusarDeImediato(
                    solicitacao.pedidoId(), solicitacao.usuarioId(), valor, forma,
                    MotivoRecusa.CARTAO_INVALIDO);
            repositorio.salvar(recusado);
            publicarEventos(recusado);
            return traduzir(recusado);
        }

        Pagamento pagamento = Pagamento.solicitar(
                solicitacao.pedidoId(), solicitacao.usuarioId(), valor, forma, cartao.get());

        pagamento.violacaoDePolitica().ifPresentOrElse(
                pagamento::recusar,
                () -> autorizarNoProcessador(pagamento, cartao.get(), valor));

        repositorio.salvar(pagamento);
        publicarEventos(pagamento);
        return traduzir(pagamento);
    }

    /**
     * Drena os fatos registrados pelo agregado e os entrega à porta de publicação.
     *
     * <p>Acontece <strong>depois</strong> de {@code salvar} e dentro da mesma transação,
     * de propósito. O adaptador do Spring segura a entrega até o commit
     * ({@code @TransactionalEventListener(AFTER_COMMIT)} do lado de quem consome), então
     * um rollback leva o evento embora junto com o estado. Publicar antes de persistir
     * anunciaria um pagamento que pode nunca ter existido.</p>
     *
     * <p>O agregado é limpo em seguida: o que já foi publicado não se publica de novo.</p>
     */
    private void publicarEventos(Pagamento pagamento) {
        List<EventoDeDominio> eventos = pagamento.eventosRegistrados();
        if (eventos.isEmpty()) {
            return;
        }
        publicador.publicar(eventos);
        pagamento.limparEventos();
    }

    private void autorizarNoProcessador(Pagamento pagamento, NumeroCartao cartao,
                                        Dinheiro valor) {
        ResultadoAutorizacao autorizacao = processador.autorizar(cartao, valor);
        if (autorizacao.autorizado()) {
            pagamento.aprovar(autorizacao.codigoAutorizacao());
        } else {
            pagamento.recusar(autorizacao.motivo());
        }
    }

    /** Traduz o agregado para o contrato de integração. */
    private ResultadoPagamento traduzir(Pagamento pagamento) {
        MotivoRecusa motivo = pagamento.motivo();
        return new ResultadoPagamento(
                pagamento.id().toString(),
                pagamento.status().name(),
                motivo == null ? null : motivo.name(),
                pagamento.codigoAutorizacao());
    }
}
