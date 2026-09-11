package br.edu.infnet.ecommerce.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(
        @NotNull Long produtoId,
        @Min(1) int quantidade
) {
}
