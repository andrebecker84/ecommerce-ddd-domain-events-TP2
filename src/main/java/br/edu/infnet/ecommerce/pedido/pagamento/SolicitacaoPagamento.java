package br.edu.infnet.ecommerce.pedido.pagamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Contrato de entrada: só identificadores e tipos primitivos.
 *
 * O contexto de Pagamento recebe {@code pedidoId} e {@code usuarioId}, nunca
 * as entidades {@code Pedido} e {@code Usuario}, como acontecia no legado.
 */
public record SolicitacaoPagamento(
        @NotNull Long pedidoId,
        @NotNull Long usuarioId,
        // sem @Positive de propósito: valor <= 0 é regra de negócio (recusa),
        // avaliada pelo agregado, e não erro de validação da fronteira
        @NotNull BigDecimal valor,
        @NotBlank String formaPagamento,
        @NotBlank String numeroCartao
) {
}
