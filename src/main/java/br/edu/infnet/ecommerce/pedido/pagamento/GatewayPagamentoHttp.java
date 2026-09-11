package br.edu.infnet.ecommerce.pedido.pagamento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fase 2 do Branch by Abstraction: mesmo contrato, agora sobre HTTP.
 *
 * Ativo quando {@code pagamento.modo=remoto}. Nenhuma linha de
 * {@code PedidoService} muda para alternar entre os dois modos, e é essa
 * reversibilidade que torna a migração segura.
 */
@Component
@ConditionalOnProperty(name = "pagamento.modo", havingValue = "remoto")
class GatewayPagamentoHttp implements GatewayPagamento {

    private final RestClient client;

    GatewayPagamentoHttp(RestClient.Builder builder,
                         @Value("${pagamento.url:http://localhost:8081}") String url) {
        this.client = builder.baseUrl(url).build();
    }

    @Override
    public ResultadoPagamento cobrar(SolicitacaoPagamento solicitacao) {
        return client.post()
                .uri("/pagamentos")
                .body(solicitacao)
                .retrieve()
                .body(ResultadoPagamento.class);
    }
}
