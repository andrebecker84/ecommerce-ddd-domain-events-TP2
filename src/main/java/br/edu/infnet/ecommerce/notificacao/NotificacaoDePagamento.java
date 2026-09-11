package br.edu.infnet.ecommerce.notificacao;

import br.edu.infnet.ecommerce.pagamento.domain.evento.PagamentoAprovado;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PagamentoRecusado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Consumidor de exemplo: o lado de Notificações reagindo aos fatos de Pagamento.
 *
 * <p>Existe para mostrar o outro extremo da publicação. Repare no que <strong>não</strong>
 * acontece aqui: esta classe não conhece o agregado {@code Pagamento}, não chama o
 * contexto de Pagamento e não é chamada por ele. Ela declara interesse em um fato e
 * recebe o fato, e nada mais. Acrescentar um segundo consumidor (Fiscal, Pedido) não exige
 * mudança nenhuma no publicador; é a diferença prática entre integrar por evento e
 * integrar por chamada direta.</p>
 *
 * <p><strong>{@code AFTER_COMMIT} é a parte importante.</strong> O evento chega apenas
 * depois que a transação do pagamento confirmou. Se ela sofrer rollback, o e-mail não
 * sai, o que evita o pior defeito desse tipo de integração: avisar o cliente de uma
 * aprovação que o banco desfez. Em compensação, a entrega passa a ser "no máximo uma
 * vez": commit seguido de queda do processo perde o aviso. Quando essa perda for
 * inaceitável, a resposta é o padrão <em>outbox</em>: gravar o evento na mesma
 * transação e deixar um processo separado publicá-lo.</p>
 */
@Component
public class NotificacaoDePagamento {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoDePagamento.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void quandoPagamentoAprovado(PagamentoAprovado evento) {
        log.info("[{}] pedido {} pago: {} {}, avisando o cliente {} (evento {}, ocorrido em {})",
                evento.nome(), evento.pedidoId(), evento.moeda(), evento.valor(),
                evento.usuarioId(), evento.eventoId(), evento.ocorridoEm());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void quandoPagamentoRecusado(PagamentoRecusado evento) {
        log.info("[{}] pedido {} não pago ({}), avisando o cliente {} (evento {})",
                evento.nome(), evento.pedidoId(), evento.motivo(), evento.usuarioId(),
                evento.eventoId());
    }
}
