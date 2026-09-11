package br.edu.infnet.ecommerce.pedido.pagamento;

/**
 * Abstração criada no lado do consumidor (Branch by Abstraction).
 *
 * O contexto de Pedido depende apenas desta interface e dos dois records deste
 * pacote. Nenhuma classe do contexto de Pagamento (agregado, Value Object,
 * enum ou entidade JPA) aparece na assinatura, e é isso que permite trocar a
 * implementação local pela remota sem tocar em {@code PedidoService}.
 */
public interface GatewayPagamento {

    ResultadoPagamento cobrar(SolicitacaoPagamento solicitacao);
}
