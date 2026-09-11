package br.edu.infnet.ecommerce.response;

import br.edu.infnet.ecommerce.entity.ItemPedido;

import java.math.BigDecimal;

/** Item de um pedido na resposta da API. */
public record ItemPedidoResponse(
        Long produtoId,
        String produtoNome,
        int quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {

    public static ItemPedidoResponse de(ItemPedido item) {
        return new ItemPedidoResponse(
                item.getProduto().getId(),
                item.getProduto().getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getSubtotal());
    }
}
