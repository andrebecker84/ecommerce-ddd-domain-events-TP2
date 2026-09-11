package br.edu.infnet.ecommerce.pagamento.infrastructure.persistence;

import br.edu.infnet.ecommerce.pagamento.domain.Pagamento;
import br.edu.infnet.ecommerce.pagamento.domain.PagamentoId;
import br.edu.infnet.ecommerce.pagamento.domain.PagamentoRepositorio;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Adaptador que implementa a porta de persistência do domínio sobre JPA. */
@Repository
class PagamentoRepositorioJpa implements PagamentoRepositorio {

    private final PagamentoJpaRepository jpa;

    PagamentoRepositorioJpa(PagamentoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void salvar(Pagamento pagamento) {
        jpa.save(PagamentoJpaMapper.paraEntidade(pagamento));
    }

    @Override
    public Optional<Pagamento> porId(PagamentoId id) {
        return jpa.findById(id.valor()).map(PagamentoJpaMapper::paraDominio);
    }

    @Override
    public Optional<Pagamento> porPedido(long pedidoId) {
        return jpa.findByPedidoId(pedidoId).map(PagamentoJpaMapper::paraDominio);
    }
}
