package br.edu.infnet.ecommerce.service;

import br.edu.infnet.ecommerce.entity.Estoque;
import br.edu.infnet.ecommerce.entity.ItemPedido;
import br.edu.infnet.ecommerce.entity.Pedido;
import br.edu.infnet.ecommerce.entity.Produto;
import br.edu.infnet.ecommerce.entity.Usuario;
import br.edu.infnet.ecommerce.exception.EstoqueInsuficienteException;
import br.edu.infnet.ecommerce.exception.PagamentoRecusadoException;
import br.edu.infnet.ecommerce.exception.RecursoNaoEncontradoException;
import br.edu.infnet.ecommerce.pedido.pagamento.GatewayPagamento;
import br.edu.infnet.ecommerce.pedido.pagamento.ResultadoPagamento;
import br.edu.infnet.ecommerce.pedido.pagamento.SolicitacaoPagamento;
import br.edu.infnet.ecommerce.repository.EstoqueRepository;
import br.edu.infnet.ecommerce.repository.PedidoRepository;
import br.edu.infnet.ecommerce.repository.ProdutoRepository;
import br.edu.infnet.ecommerce.repository.UsuarioRepository;
import br.edu.infnet.ecommerce.request.CriarPedidoRequest;
import br.edu.infnet.ecommerce.request.ItemPedidoRequest;
import br.edu.infnet.ecommerce.response.PedidoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    /*
     * Depois da extração do contexto de Pagamento:
     *
     * - PagamentoService e PagamentoRepository saíram das dependências;
     * - o pagamento é acionado por GatewayPagamento, uma abstração que trafega
     *   apenas identificadores e tipos primitivos;
     * - nenhuma classe do contexto de Pagamento aparece nos imports acima.
     *
     * O acoplamento que resta é o de domínio: o pedido precisa ser pago para
     * ser concluído, e isso é uma regra do negócio, não da implementação.
     */
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final PedidoRepository pedidoRepository;
    private final GatewayPagamento gatewayPagamento;

    public PedidoService(
            UsuarioRepository usuarioRepository,
            ProdutoRepository produtoRepository,
            EstoqueRepository estoqueRepository,
            PedidoRepository pedidoRepository,
            GatewayPagamento gatewayPagamento
    ) {
        this.usuarioRepository = usuarioRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
        this.pedidoRepository = pedidoRepository;
        this.gatewayPagamento = gatewayPagamento;
    }

    /*
     * As consultas devolvem DTOs, não entidades. A conversão acontece dentro
     * da transação, com a coleção de itens já carregada pelo "join fetch",
     * é isso que permite manter spring.jpa.open-in-view=false.
     */
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return pedidoRepository.buscarTodosComItens()
                .stream()
                .map(PedidoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Long id) {
        return pedidoRepository.buscarComItens(id)
                .map(PedidoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Pedido não encontrado: " + id
                ));
    }

    @Transactional
    public PedidoResponse criar(CriarPedidoRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Usuário não encontrado: " + request.usuarioId()
                ));

        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException("Usuário inativo");
        }

        Pedido pedido = new Pedido(usuario);
        BigDecimal total = BigDecimal.ZERO;

        for (ItemPedidoRequest itemRequest : request.itens()) {
            Produto produto = produtoRepository.findById(itemRequest.produtoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Produto não encontrado: " + itemRequest.produtoId()
                    ));

            if (!produto.isAtivo()) {
                throw new IllegalArgumentException(
                        "Produto inativo: " + produto.getNome()
                );
            }

            Estoque estoque = estoqueRepository.findByProdutoId(produto.getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException(
                            "Estoque não encontrado para o produto: " + produto.getId()
                    ));

            if (estoque.getQuantidade() < itemRequest.quantidade()) {
                throw new EstoqueInsuficienteException(
                        "Estoque insuficiente para o produto: " + produto.getNome()
                );
            }

            estoque.setQuantidade(
                    estoque.getQuantidade() - itemRequest.quantidade()
            );
            estoqueRepository.save(estoque);

            ItemPedido item = new ItemPedido(
                    produto,
                    itemRequest.quantidade(),
                    produto.getPreco()
            );

            pedido.adicionarItem(item);
            total = total.add(item.getSubtotal());
        }

        pedido.setValorTotal(total);
        pedido.setStatus("AGUARDANDO_PAGAMENTO");
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // Única porta de saída para o contexto de Pagamento.
        ResultadoPagamento resultado = gatewayPagamento.cobrar(
                new SolicitacaoPagamento(
                        pedidoSalvo.getId(),
                        usuario.getId(),
                        total,
                        request.formaPagamento(),
                        request.numeroCartao()
                )
        );

        if (!resultado.aprovado()) {
            pedidoSalvo.setStatus("PAGAMENTO_RECUSADO");
            pedidoRepository.save(pedidoSalvo);

            throw new PagamentoRecusadoException(
                    "Pagamento recusado: " + resultado.motivo()
            );
        }

        pedidoSalvo.setStatus("PAGO");
        return PedidoResponse.de(pedidoRepository.save(pedidoSalvo));
    }
}
