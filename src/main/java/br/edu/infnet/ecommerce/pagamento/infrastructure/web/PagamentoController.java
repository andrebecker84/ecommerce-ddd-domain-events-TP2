package br.edu.infnet.ecommerce.pagamento.infrastructure.web;

import br.edu.infnet.ecommerce.pagamento.application.PagamentoAppService;
import br.edu.infnet.ecommerce.pedido.pagamento.ResultadoPagamento;
import br.edu.infnet.ecommerce.pedido.pagamento.SolicitacaoPagamento;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta de entrada do contexto de Pagamento.
 *
 * É a mesma rota que o {@code GatewayPagamentoHttp} consumirá quando o
 * contexto for promovido a um processo separado, porque o contrato já está publicado.
 */
@RestController
@RequestMapping("/pagamentos")
class PagamentoController {

    private final PagamentoAppService servico;

    PagamentoController(PagamentoAppService servico) {
        this.servico = servico;
    }

    @PostMapping
    ResultadoPagamento processar(@RequestBody @Valid SolicitacaoPagamento solicitacao) {
        return servico.processar(solicitacao);
    }
}
