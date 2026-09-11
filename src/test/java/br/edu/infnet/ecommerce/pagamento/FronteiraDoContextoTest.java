package br.edu.infnet.ecommerce.pagamento;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica a fronteira do contexto lendo o próprio código-fonte.
 *
 * É uma trava barata contra a regressão mais provável desta refatoração:
 * alguém "resolver" um problema injetando de volta um repositório do monólito
 * dentro do contexto de Pagamento. O teste falha antes do code review.
 */
class FronteiraDoContextoTest {

    private static final Path CONTEXTO =
            Path.of("src", "main", "java", "br", "edu", "infnet", "ecommerce", "pagamento");

    private static final List<String> PROIBIDOS = List.of(
            "UsuarioRepository",
            "ProdutoRepository",
            "EstoqueRepository",
            "PedidoRepository");

    @Test
    @DisplayName("o contexto de Pagamento não acessa os repositórios do monólito")
    void naoAcessaRepositoriosDoMonolito() throws IOException {
        List<String> violacoes = new ArrayList<>();

        for (Path arquivo : arquivosJava()) {
            String codigo = semComentarios(Files.readString(arquivo));
            for (String proibido : PROIBIDOS) {
                if (codigo.contains(proibido)) {
                    violacoes.add(arquivo + " referencia " + proibido);
                }
            }
        }

        assertTrue(violacoes.isEmpty(),
                "O contexto de Pagamento deve receber apenas identificadores. Violações: "
                        + violacoes);
    }

    @Test
    @DisplayName("o domínio do contexto não depende de Spring nem de JPA")
    void dominioLivreDeFramework() throws IOException {
        List<String> violacoes = new ArrayList<>();

        for (Path arquivo : arquivosJava()) {
            if (!arquivo.toString().contains("domain")) {
                continue;
            }
            String codigo = semComentarios(Files.readString(arquivo));
            if (codigo.contains("org.springframework") || codigo.contains("jakarta.persistence")) {
                violacoes.add(arquivo.toString());
            }
        }

        assertTrue(violacoes.isEmpty(),
                "O modelo de domínio deve ser POJO. Violações: " + violacoes);
    }

    /**
     * Remove comentários antes da verificação: o que importa é o código, e um
     * Javadoc que explica a restrição não pode fazer o teste falhar.
     */
    private static String semComentarios(String codigo) {
        return codigo
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "");
    }

    private static List<Path> arquivosJava() throws IOException {
        try (Stream<Path> caminhos = Files.walk(CONTEXTO)) {
            return caminhos.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }
}
