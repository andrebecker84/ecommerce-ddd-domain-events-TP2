package br.edu.infnet.ecommerce.repository;

import br.edu.infnet.ecommerce.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /*
     * As consultas abaixo usam "join fetch" para trazer itens, produtos e
     * usuário na mesma ida ao banco. Sem elas, montar a resposta dispararia
     * uma consulta por pedido para carregar os itens, o conhecido problema N+1.
     */

    @Query("""
            select distinct p from Pedido p
              join fetch p.usuario
              left join fetch p.itens i
              left join fetch i.produto
            """)
    List<Pedido> buscarTodosComItens();

    @Query("""
            select p from Pedido p
              join fetch p.usuario
              left join fetch p.itens i
              left join fetch i.produto
            where p.id = :id
            """)
    Optional<Pedido> buscarComItens(Long id);
}
