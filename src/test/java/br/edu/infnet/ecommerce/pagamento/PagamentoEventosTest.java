package br.edu.infnet.ecommerce.pagamento;

import br.edu.infnet.ecommerce.pagamento.domain.Dinheiro;
import br.edu.infnet.ecommerce.pagamento.domain.FormaPagamento;
import br.edu.infnet.ecommerce.pagamento.domain.MotivoRecusa;
import br.edu.infnet.ecommerce.pagamento.domain.NumeroCartao;
import br.edu.infnet.ecommerce.pagamento.domain.Pagamento;
import br.edu.infnet.ecommerce.pagamento.domain.evento.EventoDeDominio;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PagamentoAprovado;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PagamentoRecusado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Registro de eventos de domínio no agregado.
 *
 * O que estes testes protegem é a ordem do método de negócio: a invariante decide
 * primeiro, o estado muda depois, e só então o fato é registrado. Um evento
 * registrado antes da validação seria o anúncio de algo que não aconteceu.
 */
class PagamentoEventosTest {

    private static final long PEDIDO_ID = 42L;
    private static final long USUARIO_ID = 7L;

    private Pagamento pagamentoPendente() {
        return Pagamento.solicitar(PEDIDO_ID, USUARIO_ID, Dinheiro.reais(new BigDecimal("250.00")),
                FormaPagamento.CARTAO, NumeroCartao.tentar("4111111111111111").orElseThrow());
    }

    @Test
    @DisplayName("pagamento recém-solicitado não registrou nenhum fato")
    void nasceSemEventos() {
        assertTrue(pagamentoPendente().eventosRegistrados().isEmpty());
    }

    @Test
    @DisplayName("aprovar registra PagamentoAprovado com os dados do pagamento")
    void aprovarRegistraEvento() {
        Pagamento pagamento = pagamentoPendente();

        pagamento.aprovar("AUTH-123");

        List<EventoDeDominio> eventos = pagamento.eventosRegistrados();
        assertEquals(1, eventos.size());

        PagamentoAprovado evento = assertInstanceOf(PagamentoAprovado.class, eventos.getFirst());
        assertEquals(pagamento.id().toString(), evento.agregadoId());
        assertEquals(PEDIDO_ID, evento.pedidoId());
        assertEquals(USUARIO_ID, evento.usuarioId());
        assertEquals(new BigDecimal("250.00"), evento.valor());
        assertEquals("BRL", evento.moeda());
        assertEquals("AUTH-123", evento.codigoAutorizacao());
        assertNotNull(evento.eventoId());
        assertNotNull(evento.ocorridoEm());
        assertEquals("PagamentoAprovado", evento.nome());
    }

    @Test
    @DisplayName("recusar registra PagamentoRecusado com o motivo")
    void recusarRegistraEvento() {
        Pagamento pagamento = pagamentoPendente();

        pagamento.recusar(MotivoRecusa.LIMITE_EXCEDIDO);

        PagamentoRecusado evento = assertInstanceOf(PagamentoRecusado.class,
                pagamento.eventosRegistrados().getFirst());
        assertEquals("LIMITE_EXCEDIDO", evento.motivo());
        assertEquals(pagamento.id().toString(), evento.agregadoId());
    }

    @Test
    @DisplayName("pagamento recusado de imediato já nasce com o fato registrado")
    void recusaImediataRegistraEvento() {
        Pagamento pagamento = Pagamento.recusarDeImediato(PEDIDO_ID, USUARIO_ID,
                Dinheiro.reais(new BigDecimal("99.90")), FormaPagamento.CARTAO,
                MotivoRecusa.CARTAO_INVALIDO);

        PagamentoRecusado evento = assertInstanceOf(PagamentoRecusado.class,
                pagamento.eventosRegistrados().getFirst());
        assertEquals("CARTAO_INVALIDO", evento.motivo());
    }

    @Test
    @DisplayName("invariante violada não registra evento: aprovar duas vezes falha e o fato continua único")
    void invarianteVioladaNaoRegistraSegundoEvento() {
        Pagamento pagamento = pagamentoPendente();
        pagamento.aprovar("AUTH-123");

        assertThrows(IllegalStateException.class, () -> pagamento.aprovar("AUTH-456"));
        assertThrows(IllegalStateException.class, () -> pagamento.recusar(MotivoRecusa.CARTAO_BLOQUEADO));

        assertEquals(1, pagamento.eventosRegistrados().size());
    }

    @Test
    @DisplayName("a lista devolvida é cópia imutável, e ninguém registra fato por fora do agregado")
    void listaDeEventosEhImutavel() {
        Pagamento pagamento = pagamentoPendente();
        pagamento.aprovar("AUTH-123");

        List<EventoDeDominio> eventos = pagamento.eventosRegistrados();

        assertThrows(UnsupportedOperationException.class, () -> eventos.add(null));
    }

    @Test
    @DisplayName("limparEventos descarta o que já foi publicado")
    void limparEventosEsvaziaAFila() {
        Pagamento pagamento = pagamentoPendente();
        pagamento.aprovar("AUTH-123");

        pagamento.limparEventos();

        assertTrue(pagamento.eventosRegistrados().isEmpty());
    }
}
