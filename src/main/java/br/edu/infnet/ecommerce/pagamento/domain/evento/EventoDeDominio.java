package br.edu.infnet.ecommerce.pagamento.domain.evento;

import java.time.Instant;
import java.util.UUID;

/**
 * Abstração que padroniza o contrato de todo evento de domínio do contexto.
 *
 * <p>Um evento de domínio é um <strong>fato consumado</strong>: algo que já aconteceu
 * e que outras partes do sistema podem querer saber. Daí as três propriedades que
 * esta interface exige de qualquer implementação:</p>
 *
 * <ul>
 *   <li>{@link #eventoId()}, a identidade própria do evento, distinta da identidade do
 *       agregado. É o que permite a um consumidor perceber que já tratou aquele fato
 *       quando a mesma mensagem chega duas vezes;</li>
 *   <li>{@link #ocorridoEm()}, o momento em que o fato aconteceu, não o momento em que
 *       foi entregue. Um evento pode ser publicado, transportado e consumido muito
 *       depois; sem esse carimbo, a ordem dos fatos se perde;</li>
 *   <li>{@link #agregadoId()}, de quem é o fato. Referência por identidade, nunca o
 *       agregado inteiro: quem consome está fora da transação e, possivelmente, fora
 *       do processo.</li>
 * </ul>
 *
 * <p>Implementações devem ser <strong>imutáveis</strong>, e por isso os eventos deste
 * contexto são {@code record}, além de conter apenas tipos serializáveis. Nada de Spring,
 * de JPA ou de objetos do modelo aqui dentro: o evento é um contrato, e contrato que
 * arrasta framework junto não atravessa fronteira de processo.</p>
 *
 * <p>Por convenção, o nome da implementação vai no <strong>passado</strong>
 * ({@code PagamentoAprovado}, não {@code AprovarPagamento}): comando é pedido, que pode
 * ser recusado; evento é fato, que já ocorreu e não se nega.</p>
 */
public interface EventoDeDominio {

    /** Identidade do evento, que permite deduplicar na entrega. */
    UUID eventoId();

    /** Momento em que o fato ocorreu, em UTC. */
    Instant ocorridoEm();

    /** Identificador do agregado que originou o fato. */
    String agregadoId();

    /**
     * Nome do fato, usado como chave de roteamento na publicação.
     *
     * <p>O padrão é o nome simples da classe, o que basta para este contexto.
     * Se um dia o evento cruzar processo, é aqui que entra a versão do contrato
     * (por exemplo, {@code PagamentoAprovado.v2}).</p>
     */
    default String nome() {
        return getClass().getSimpleName();
    }
}
