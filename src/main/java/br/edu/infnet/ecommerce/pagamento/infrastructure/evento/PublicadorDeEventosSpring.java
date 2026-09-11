package br.edu.infnet.ecommerce.pagamento.infrastructure.evento;

import br.edu.infnet.ecommerce.pagamento.domain.evento.EventoDeDominio;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PublicadorDeEventos;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptador de saída: publica os eventos no barramento em memória do Spring.
 *
 * <p>É o equivalente, dentro de um processo só, ao produtor de um tópico. O
 * {@code ApplicationEventPublisher} entrega a <strong>todos</strong> os interessados,
 * numa semântica de tópico e não de fila, e cada consumidor decide o que fazer com o fato.</p>
 *
 * <p>Esta classe é a única do contexto que conhece o mecanismo de entrega. Quando o
 * e-commerce precisar publicar para fora do processo, é aqui que entra o cliente do
 * broker, e é só aqui.</p>
 */
@Component
public class PublicadorDeEventosSpring implements PublicadorDeEventos {

    private final ApplicationEventPublisher publisher;

    public PublicadorDeEventosSpring(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publicar(List<EventoDeDominio> eventos) {
        eventos.forEach(publisher::publishEvent);
    }
}
