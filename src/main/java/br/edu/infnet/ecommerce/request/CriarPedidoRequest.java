package br.edu.infnet.ecommerce.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CriarPedidoRequest(
        @NotNull Long usuarioId,
        @NotEmpty List<@Valid ItemPedidoRequest> itens,
        @NotBlank String formaPagamento,
        @NotBlank String numeroCartao
) {
}
