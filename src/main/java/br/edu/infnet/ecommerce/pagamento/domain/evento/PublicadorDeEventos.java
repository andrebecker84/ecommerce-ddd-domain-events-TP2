package br.edu.infnet.ecommerce.pagamento.domain.evento;

import java.util.List;

/**
 * Porta de saída para a publicação dos eventos de domínio.
 *
 * <p>Declarada na linguagem do domínio, como {@code PagamentoRepositorio} e
 * {@code ProcessadorCartao}. O agregado registra os fatos; o serviço de aplicação os
 * drena e entrega a esta porta; o adaptador decide como eles viajam.</p>
 *
 * <p>É essa indireção que mantém o domínio POJO. Hoje o adaptador é o publicador de
 * eventos do Spring, dentro do mesmo processo. Trocá-lo por um produtor de tópico
 * (Kafka, RabbitMQ, SNS) não muda uma linha do agregado nem do serviço de aplicação:
 * muda a classe que implementa esta interface.</p>
 */
public interface PublicadorDeEventos {

    /** Publica os fatos na ordem em que ocorreram. */
    void publicar(List<EventoDeDominio> eventos);
}
