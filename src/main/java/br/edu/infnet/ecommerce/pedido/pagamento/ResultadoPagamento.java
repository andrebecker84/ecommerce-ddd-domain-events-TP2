package br.edu.infnet.ecommerce.pedido.pagamento;

/**
 * Contrato de saída: nada do modelo interno de Pagamento vaza.
 *
 * Os campos são strings justamente para que o contexto de Pedido não precise
 * conhecer os enums do outro contexto. A serialização HTTP, quando o serviço
 * for extraído, usa exatamente este mesmo formato.
 */
public record ResultadoPagamento(String pagamentoId,
                                 String status,
                                 String motivo,
                                 String codigoAutorizacao) {

    public boolean aprovado() {
        return "APROVADO".equals(status);
    }
}
