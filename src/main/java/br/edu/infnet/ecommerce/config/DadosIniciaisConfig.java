package br.edu.infnet.ecommerce.config;

import br.edu.infnet.ecommerce.entity.Estoque;
import br.edu.infnet.ecommerce.entity.Produto;
import br.edu.infnet.ecommerce.entity.Usuario;
import br.edu.infnet.ecommerce.repository.EstoqueRepository;
import br.edu.infnet.ecommerce.repository.ProdutoRepository;
import br.edu.infnet.ecommerce.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DadosIniciaisConfig {

    @Bean
    CommandLineRunner carregarDados(
            UsuarioRepository usuarioRepository,
            ProdutoRepository produtoRepository,
            EstoqueRepository estoqueRepository
    ) {
        return args -> {
            usuarioRepository.save(
                    new Usuario("Ana Souza", "ana@exemplo.com", true)
            );
            usuarioRepository.save(
                    new Usuario("Bruno Lima", "bruno@exemplo.com", true)
            );
            usuarioRepository.save(
                    new Usuario("Carlos Inativo", "carlos@exemplo.com", false)
            );

            Produto notebook = produtoRepository.save(
                    new Produto(
                            "Notebook",
                            "Notebook para uso profissional",
                            new BigDecimal("4500.00"),
                            true
                    )
            );

            Produto teclado = produtoRepository.save(
                    new Produto(
                            "Teclado mecânico",
                            "Teclado ABNT2",
                            new BigDecimal("350.00"),
                            true
                    )
            );

            Produto mouse = produtoRepository.save(
                    new Produto(
                            "Mouse",
                            "Mouse sem fio",
                            new BigDecimal("150.00"),
                            true
                    )
            );

            Produto servidor = produtoRepository.save(
                    new Produto(
                            "Servidor",
                            "Servidor de alto desempenho",
                            new BigDecimal("7500.00"),
                            true
                    )
            );

            Produto produtoInativo = produtoRepository.save(
                    new Produto(
                            "Produto descontinuado",
                            "Produto não disponível",
                            new BigDecimal("99.00"),
                            false
                    )
            );

            estoqueRepository.save(new Estoque(notebook, 10));
            estoqueRepository.save(new Estoque(teclado, 25));
            estoqueRepository.save(new Estoque(mouse, 50));
            estoqueRepository.save(new Estoque(servidor, 5));
            estoqueRepository.save(new Estoque(produtoInativo, 100));
        };
    }
}
