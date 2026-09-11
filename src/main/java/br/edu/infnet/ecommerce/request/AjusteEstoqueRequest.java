package br.edu.infnet.ecommerce.request;

import jakarta.validation.constraints.NotNull;

public record AjusteEstoqueRequest(
        @NotNull Integer quantidade
) {
}
