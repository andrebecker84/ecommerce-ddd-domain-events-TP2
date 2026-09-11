package br.edu.infnet.ecommerce.pagamento.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data restrito à infraestrutura: o domínio nunca enxerga este tipo. */
interface PagamentoJpaRepository extends JpaRepository<PagamentoJpaEntity, UUID> {

    Optional<PagamentoJpaEntity> findByPedidoId(Long pedidoId);
}
