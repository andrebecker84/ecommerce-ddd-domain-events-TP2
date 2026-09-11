package br.edu.infnet.ecommerce.pagamento.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Objeto de Valor que representa a identidade do agregado.
 *
 * Usar um tipo próprio em vez de {@code Long} impede que um identificador de
 * pedido ou de usuário seja passado por engano onde se espera um pagamento.
 */
public record PagamentoId(UUID valor) {

    public PagamentoId {
        Objects.requireNonNull(valor, "PagamentoId não pode ser nulo");
    }

    public static PagamentoId novo() {
        return new PagamentoId(UUID.randomUUID());
    }

    public static PagamentoId de(String valor) {
        return new PagamentoId(UUID.fromString(valor));
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
