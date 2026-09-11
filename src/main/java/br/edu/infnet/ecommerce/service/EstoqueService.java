package br.edu.infnet.ecommerce.service;

import br.edu.infnet.ecommerce.entity.Estoque;
import br.edu.infnet.ecommerce.entity.Produto;
import br.edu.infnet.ecommerce.exception.RecursoNaoEncontradoException;
import br.edu.infnet.ecommerce.repository.EstoqueRepository;
import br.edu.infnet.ecommerce.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final ProdutoRepository produtoRepository;

    public EstoqueService(
            EstoqueRepository estoqueRepository,
            ProdutoRepository produtoRepository
    ) {
        this.estoqueRepository = estoqueRepository;
        this.produtoRepository = produtoRepository;
    }

    public List<Estoque> listar() {
        return estoqueRepository.findAll();
    }

    public Estoque buscarPorProduto(Long produtoId) {
        return estoqueRepository.findByProdutoId(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Estoque não encontrado para o produto: " + produtoId
                ));
    }

    @Transactional
    public Estoque definirQuantidade(Long produtoId, int quantidade) {
        if (quantidade < 0) {
            throw new IllegalArgumentException("A quantidade não pode ser negativa");
        }

        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado: " + produtoId
                ));

        Estoque estoque = estoqueRepository.findByProdutoId(produtoId)
                .orElseGet(() -> new Estoque(produto, 0));

        estoque.setQuantidade(quantidade);
        return estoqueRepository.save(estoque);
    }
}
