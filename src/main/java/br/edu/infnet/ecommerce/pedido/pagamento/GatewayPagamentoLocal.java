package br.edu.infnet.ecommerce.pedido.pagamento;

import br.edu.infnet.ecommerce.pagamento.application.PagamentoAppService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fase 1 do Branch by Abstraction: contexto de Pagamento já separado por
 * pacote e por modelo, mas ainda no mesmo processo.
 *
 * Ativo quando {@code pagamento.modo=local} (padrão).
 */
@Component
@ConditionalOnProperty(name = "pagamento.modo", havingValue = "local", matchIfMissing = true)
class GatewayPagamentoLocal implements GatewayPagamento {

    private final PagamentoAppService servico;

    GatewayPagamentoLocal(PagamentoAppService servico) {
        this.servico = servico;
    }

    @Override
    public ResultadoPagamento cobrar(SolicitacaoPagamento solicitacao) {
        return servico.processar(solicitacao);
    }
}
