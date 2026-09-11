package br.edu.infnet.ecommerce.pagamento.domain.evento;

import br.edu.infnet.ecommerce.pagamento.domain.Dinheiro;
import br.edu.infnet.ecommerce.pagamento.domain.PagamentoId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Fato: um pagamento foi aprovado.
 *
 * <p>É o evento que o resto do e-commerce espera. O contexto de Pedido marca o pedido
 * como pago, Notificações avisa o cliente e Fiscal emite a nota. Nenhum deles precisa
 * consultar o Pagamento para saber disso, e o Pagamento não precisa conhecer nenhum
 * deles.</p>
 *
 * <p>O evento carrega <strong>primitivos</strong>, não os Objetos de Valor do domínio:
 * {@code BigDecimal} e o código da moeda no lugar de {@link Dinheiro}, {@code String} no
 * lugar de {@link PagamentoId}. Quem consome pode estar em outro processo, escrito em
 * outra linguagem; um contrato que exige as classes deste pacote no classpath não
 * atravessa essa fronteira. A conversão acontece na fábrica {@link #de}, que é o único
 * ponto onde o evento conhece o modelo.</p>
 *
 * <p>O número do cartão <strong>não</strong> viaja, nem mascarado. Evento é publicado em
 * tópico e lido por quem se inscrever; dado sensível só deve ir a quem precisa dele.</p>
 */
public record PagamentoAprovado(UUID eventoId,
                                Instant ocorridoEm,
                                String agregadoId,
                                long pedidoId,
                                long usuarioId,
                                BigDecimal valor,
                                String moeda,
                                String codigoAutorizacao) implements EventoDeDominio {

    public PagamentoAprovado {
        Objects.requireNonNull(eventoId, "eventoId é obrigatório");
        Objects.requireNonNull(ocorridoEm, "ocorridoEm é obrigatório");
        Objects.requireNonNull(agregadoId, "agregadoId é obrigatório");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(moeda, "moeda é obrigatória");
        Objects.requireNonNull(codigoAutorizacao, "código de autorização é obrigatório");
    }

    /** Fábrica usada pelo agregado no momento da aprovação. */
    public static PagamentoAprovado de(PagamentoId pagamentoId, long pedidoId, long usuarioId,
                                       Dinheiro valor, String codigoAutorizacao) {
        return new PagamentoAprovado(
                UUID.randomUUID(),
                Instant.now(),
                pagamentoId.toString(),
                pedidoId,
                usuarioId,
                valor.valor(),
                valor.moeda().getCurrencyCode(),
                codigoAutorizacao);
    }
}
