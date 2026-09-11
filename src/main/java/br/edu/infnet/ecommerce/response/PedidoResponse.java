package br.edu.infnet.ecommerce.response;

import br.edu.infnet.ecommerce.entity.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Representação de saída de um pedido.
 *
 * Existe para que a API não serialize entidades JPA diretamente. Com isso,
 * a sessão do Hibernate não precisa sobreviver à camada web
 * ({@code spring.jpa.open-in-view=false}), o contrato da API deixa de
 * depender do mapeamento objeto-relacional, e nenhum campo interno vaza por
 * acidente ao acrescentar uma coluna na entidade.
 */
public record PedidoResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        BigDecimal valorTotal,
        String status,
        LocalDateTime criadoEm,
        List<ItemPedidoResponse> itens
) {

    /** Converte dentro da transação, com a coleção já carregada. */
    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(
                pedido.getId(),
                pedido.getUsuario().getId(),
                pedido.getUsuario().getNome(),
                pedido.getValorTotal(),
                pedido.getStatus(),
                pedido.getCriadoEm(),
                pedido.getItens().stream().map(ItemPedidoResponse::de).toList());
    }
}
