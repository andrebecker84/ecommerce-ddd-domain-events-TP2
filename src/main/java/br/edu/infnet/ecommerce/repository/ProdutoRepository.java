package br.edu.infnet.ecommerce.repository;

import br.edu.infnet.ecommerce.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
