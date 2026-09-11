package br.edu.infnet.ecommerce.pagamento.domain.evento;

import br.edu.infnet.ecommerce.pagamento.domain.Dinheiro;
import br.edu.infnet.ecommerce.pagamento.domain.MotivoRecusa;
import br.edu.infnet.ecommerce.pagamento.domain.PagamentoId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Fato: um pagamento foi recusado.
 *
 * <p>Existe pelo mesmo motivo que {@link PagamentoAprovado}, e serve para mostrar que a
 * abstração {@link EventoDeDominio} padroniza mesmo: dois fatos diferentes, o mesmo
 * contrato de identidade, momento e origem.</p>
 *
 * <p>O motivo viaja como <strong>texto</strong>, e não como {@link MotivoRecusa}, pela
 * mesma razão que o valor viaja como {@code BigDecimal}: o consumidor não deve depender
 * das classes deste contexto. Um enum novo do lado do publicador também não pode quebrar
 * a desserialização do lado de quem consome.</p>
 */
public record PagamentoRecusado(UUID eventoId,
                                Instant ocorridoEm,
                                String agregadoId,
                                long pedidoId,
                                long usuarioId,
                                BigDecimal valor,
                                String moeda,
                                String motivo) implements EventoDeDominio {

    public PagamentoRecusado {
        Objects.requireNonNull(eventoId, "eventoId é obrigatório");
        Objects.requireNonNull(ocorridoEm, "ocorridoEm é obrigatório");
        Objects.requireNonNull(agregadoId, "agregadoId é obrigatório");
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(moeda, "moeda é obrigatória");
        Objects.requireNonNull(motivo, "motivo é obrigatório");
    }

    /** Fábrica usada pelo agregado no momento da recusa. */
    public static PagamentoRecusado de(PagamentoId pagamentoId, long pedidoId, long usuarioId,
                                       Dinheiro valor, MotivoRecusa motivo) {
        return new PagamentoRecusado(
                UUID.randomUUID(),
                Instant.now(),
                pagamentoId.toString(),
                pedidoId,
                usuarioId,
                valor.valor(),
                valor.moeda().getCurrencyCode(),
                motivo.name());
    }
}
