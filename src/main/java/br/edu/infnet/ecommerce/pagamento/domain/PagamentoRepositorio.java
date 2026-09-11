package br.edu.infnet.ecommerce.pagamento.domain;

import java.util.Optional;

/**
 * Porta de saída de persistência, declarada na linguagem do domínio.
 *
 * Trabalha com o agregado, não com entidade JPA. A implementação vive em
 * {@code infrastructure.persistence} e pode ser trocada sem afetar as regras.
 */
public interface PagamentoRepositorio {

    void salvar(Pagamento pagamento);

    Optional<Pagamento> porId(PagamentoId id);

    Optional<Pagamento> porPedido(long pedidoId);
}
