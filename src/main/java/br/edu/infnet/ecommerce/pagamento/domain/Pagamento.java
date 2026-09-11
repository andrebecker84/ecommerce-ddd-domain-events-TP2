package br.edu.infnet.ecommerce.pagamento.domain;

import br.edu.infnet.ecommerce.pagamento.domain.evento.EventoDeDominio;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PagamentoAprovado;
import br.edu.infnet.ecommerce.pagamento.domain.evento.PagamentoRecusado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate Root do contexto de Pagamento.
 *
 * Invariantes garantidos aqui dentro:
 * <ul>
 *   <li>só existe pagamento com valor e cartão válidos (garantido pelos Value Objects);</li>
 *   <li>um pagamento só pode ser aprovado ou recusado uma única vez;</li>
 *   <li>a política de valor, forma e limite vem antes de qualquer chamada externa.</li>
 * </ul>
 *
 * Pedido e Usuário são referenciados apenas por identidade. Nenhuma classe de
 * outro contexto aparece neste arquivo, e é isso que permite mover o pacote
 * inteiro para outro processo sem reescrever o domínio.
 *
 * <p><strong>Eventos de domínio.</strong> Toda transição de estado deste agregado
 * registra o fato correspondente em {@link #eventos}. O agregado apenas
 * <em>acumula</em>: ele não conhece publicador, fila nem tópico, e continua POJO
 * puro. Quem drena a lista e publica é o serviço de aplicação, depois que a
 * transação confirma, porque evento de algo que não foi persistido é pior que evento
 * nenhum.</p>
 */
public class Pagamento {

    private static final Dinheiro LIMITE_POR_TRANSACAO =
            Dinheiro.reais(new BigDecimal("10000.00"));

    /** Máscara usada quando o cartão não pôde ser interpretado. */
    private static final String CARTAO_NAO_IDENTIFICADO = "****";

    private final PagamentoId id;
    private final long pedidoId;     // outro agregado: referência por identidade
    private final long usuarioId;    // outro agregado: referência por identidade
    private final Dinheiro valor;
    private final FormaPagamento forma;
    private final String cartaoMascarado;
    private final LocalDateTime criadoEm;

    private StatusPagamento status;
    private MotivoRecusa motivo;
    private String codigoAutorizacao;
    private LocalDateTime processadoEm;

    /** Fatos ocorridos neste agregado e ainda não publicados. */
    private final List<EventoDeDominio> eventos = new ArrayList<>();

    private Pagamento(PagamentoId id, long pedidoId, long usuarioId, Dinheiro valor,
                      FormaPagamento forma, String cartaoMascarado, StatusPagamento status,
                      LocalDateTime criadoEm) {
        this.id = Objects.requireNonNull(id, "id é obrigatório");
        this.pedidoId = pedidoId;
        this.usuarioId = usuarioId;
        this.valor = Objects.requireNonNull(valor, "valor é obrigatório");
        this.forma = Objects.requireNonNull(forma, "forma de pagamento é obrigatória");
        this.cartaoMascarado = cartaoMascarado;
        this.status = Objects.requireNonNull(status);
        this.criadoEm = Objects.requireNonNull(criadoEm);
    }

    /** Única forma de criar um pagamento novo. */
    public static Pagamento solicitar(long pedidoId, long usuarioId, Dinheiro valor,
                                      FormaPagamento forma, NumeroCartao cartao) {
        Objects.requireNonNull(cartao, "cartão é obrigatório");
        return new Pagamento(PagamentoId.novo(), pedidoId, usuarioId, valor, forma,
                cartao.mascarado(), StatusPagamento.PENDENTE, LocalDateTime.now());
    }

    /**
     * Pagamento que já nasce recusado porque o número do cartão não pôde
     * sequer ser interpretado. Existe para que cartão inválido continue
     * sendo uma recusa de negócio, registrada e auditável, como no
     * comportamento original do monólito, e não um erro técnico.
     */
    public static Pagamento recusarDeImediato(long pedidoId, long usuarioId, Dinheiro valor,
                                              FormaPagamento forma, MotivoRecusa motivo) {
        Pagamento pagamento = new Pagamento(PagamentoId.novo(), pedidoId, usuarioId, valor,
                forma, CARTAO_NAO_IDENTIFICADO, StatusPagamento.PENDENTE, LocalDateTime.now());
        pagamento.recusar(motivo);
        return pagamento;
    }

    /**
     * Reconstitui o agregado a partir do estado persistido.
     * Usado apenas pelo adaptador de persistência.
     */
    public static Pagamento reconstituir(PagamentoId id, long pedidoId, long usuarioId,
                                         Dinheiro valor, FormaPagamento forma,
                                         String cartaoMascarado, StatusPagamento status,
                                         MotivoRecusa motivo, String codigoAutorizacao,
                                         LocalDateTime criadoEm, LocalDateTime processadoEm) {
        Pagamento pagamento = new Pagamento(id, pedidoId, usuarioId, valor, forma,
                cartaoMascarado, status, criadoEm);
        pagamento.motivo = motivo;
        pagamento.codigoAutorizacao = codigoAutorizacao;
        pagamento.processadoEm = processadoEm;
        return pagamento;
    }

    /**
     * Regras que dependem apenas do próprio agregado, avaliadas antes de
     * qualquer integração externa. A ordem preserva a precedência do legado.
     */
    public Optional<MotivoRecusa> violacaoDePolitica() {
        if (!valor.ehPositivo()) {
            return Optional.of(MotivoRecusa.VALOR_INVALIDO);
        }
        if (forma != FormaPagamento.CARTAO) {
            return Optional.of(MotivoRecusa.FORMA_PAGAMENTO_NAO_SUPORTADA);
        }
        if (valor.maiorQue(LIMITE_POR_TRANSACAO)) {
            return Optional.of(MotivoRecusa.LIMITE_EXCEDIDO);
        }
        return Optional.empty();
    }

    /**
     * Aprova o pagamento. O método de negócio acontece em três etapas, nesta ordem:
     *
     * <ol>
     *   <li><strong>invariante</strong>: {@code exigirPendente()} recusa a operação se o
     *       pagamento já foi processado. Nada além disso acontece se a regra não passar;</li>
     *   <li><strong>mudança de estado</strong>: o agregado passa a {@code APROVADO}, com o
     *       código de autorização e o momento do processamento;</li>
     *   <li><strong>evento</strong>: só agora o fato é registrado. Registrar antes da
     *       validação anunciaria uma aprovação que pode não acontecer; registrar antes da
     *       mudança de estado descreveria um estado que ainda não existe.</li>
     * </ol>
     */
    public void aprovar(String codigoAutorizacao) {
        exigirPendente();                                                          // 1. invariante

        this.codigoAutorizacao = Objects.requireNonNull(
                codigoAutorizacao, "código de autorização é obrigatório na aprovação");
        this.status = StatusPagamento.APROVADO;                                    // 2. estado
        this.processadoEm = LocalDateTime.now();

        registrar(PagamentoAprovado.de(id, pedidoId, usuarioId, valor,             // 3. evento
                this.codigoAutorizacao));
    }

    /** Recusa o pagamento. Mesma ordem de {@link #aprovar(String)}: invariante, estado, evento. */
    public void recusar(MotivoRecusa motivo) {
        exigirPendente();                                                          // 1. invariante

        this.motivo = Objects.requireNonNull(motivo, "motivo é obrigatório na recusa");
        this.status = StatusPagamento.RECUSADO;                                    // 2. estado
        this.processadoEm = LocalDateTime.now();

        registrar(PagamentoRecusado.de(id, pedidoId, usuarioId, valor, this.motivo)); // 3. evento
    }

    private void exigirPendente() {
        if (status != StatusPagamento.PENDENTE) {
            throw new IllegalStateException("Pagamento já processado: " + id);
        }
    }

    private void registrar(EventoDeDominio evento) {
        eventos.add(evento);
    }

    /**
     * Fatos ocorridos e ainda não publicados, em ordem de ocorrência.
     *
     * <p>Devolve uma cópia imutável: quem lê não altera o estado do agregado. Para
     * descartar os eventos já despachados, use {@link #limparEventos()}.</p>
     */
    public List<EventoDeDominio> eventosRegistrados() {
        return List.copyOf(eventos);
    }

    /** Descarta os eventos já publicados pelo serviço de aplicação. */
    public void limparEventos() {
        eventos.clear();
    }

    public boolean aprovado() {
        return status == StatusPagamento.APROVADO;
    }

    public PagamentoId id()             { return id; }
    public long pedidoId()              { return pedidoId; }
    public long usuarioId()             { return usuarioId; }
    public Dinheiro valor()             { return valor; }
    public FormaPagamento forma()       { return forma; }
    public String cartaoMascarado()     { return cartaoMascarado; }
    public StatusPagamento status()     { return status; }
    public MotivoRecusa motivo()        { return motivo; }
    public String codigoAutorizacao()   { return codigoAutorizacao; }
    public LocalDateTime criadoEm()     { return criadoEm; }
    public LocalDateTime processadoEm() { return processadoEm; }
}
