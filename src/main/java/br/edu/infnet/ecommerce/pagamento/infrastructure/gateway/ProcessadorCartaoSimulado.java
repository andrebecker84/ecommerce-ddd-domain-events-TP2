package br.edu.infnet.ecommerce.pagamento.infrastructure.gateway;

import br.edu.infnet.ecommerce.pagamento.domain.Dinheiro;
import br.edu.infnet.ecommerce.pagamento.domain.MotivoRecusa;
import br.edu.infnet.ecommerce.pagamento.domain.NumeroCartao;
import br.edu.infnet.ecommerce.pagamento.domain.ProcessadorCartao;
import br.edu.infnet.ecommerce.pagamento.domain.ResultadoAutorizacao;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador simulado da porta {@link ProcessadorCartao}, equivalente ao
 * comportamento do {@code ProcessadorPagamento} do monólito.
 *
 * Só decide o que é responsabilidade do provedor: cartão bloqueado ou
 * autorizado. Valor, forma e limite são política do domínio e já foram
 * avaliados pelo agregado antes desta chamada.
 */
@Component
class ProcessadorCartaoSimulado implements ProcessadorCartao {

    @Override
    public ResultadoAutorizacao autorizar(NumeroCartao cartao, Dinheiro valor) {
        if ("0000".equals(cartao.ultimosQuatro())) {
            return ResultadoAutorizacao.negado(MotivoRecusa.CARTAO_BLOQUEADO);
        }

        // cartão terminado em 1111 e demais cartões válidos: aprovados
        return ResultadoAutorizacao.autorizado(
                UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
