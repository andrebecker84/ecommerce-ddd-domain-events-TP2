package br.edu.infnet.ecommerce.pagamento.infrastructure.persistence;

import br.edu.infnet.ecommerce.pagamento.domain.FormaPagamento;
import br.edu.infnet.ecommerce.pagamento.domain.MotivoRecusa;
import br.edu.infnet.ecommerce.pagamento.domain.StatusPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidade JPA do contexto, separada do Aggregate Root.
 *
 * Guarda apenas colunas simples, inclusive {@code pedido_id} e
 * {@code usuario_id} como números, sem {@code @ManyToOne} e sem chave
 * estrangeira para outros contextos. É essa decisão que torna a separação
 * física do banco, mais adiante, uma migração trivial.
 */
@Entity
@Table(name = "pagamentos_ctx")
class PagamentoJpaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 3)
    private String moeda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormaPagamento forma;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private MotivoRecusa motivo;

    private String cartaoMascarado;

    private String codigoAutorizacao;

    @Column(nullable = false)
    private LocalDateTime criadoEm;

    private LocalDateTime processadoEm;

    @Transient
    private boolean novo = true;

    protected PagamentoJpaEntity() {
        // exigido pelo JPA
    }

    PagamentoJpaEntity(UUID id, Long pedidoId, Long usuarioId, BigDecimal valor, String moeda,
                       FormaPagamento forma, StatusPagamento status, MotivoRecusa motivo,
                       String cartaoMascarado, String codigoAutorizacao,
                       LocalDateTime criadoEm, LocalDateTime processadoEm) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.usuarioId = usuarioId;
        this.valor = valor;
        this.moeda = moeda;
        this.forma = forma;
        this.status = status;
        this.motivo = motivo;
        this.cartaoMascarado = cartaoMascarado;
        this.codigoAutorizacao = codigoAutorizacao;
        this.criadoEm = criadoEm;
        this.processadoEm = processadoEm;
    }

    /*
     * O identificador é atribuído pelo domínio (UUID), não gerado pelo banco.
     * Sem Persistable, o Spring Data trataria a entidade como existente e
     * faria um SELECT antes de cada INSERT. O agregado só é persistido uma
     * vez, já que ele recusa ser processado duas vezes, então declarar que a
     * instância é sempre nova é correto e economiza uma ida ao banco.
     */
    @Override
    public boolean isNew() {
        return novo;
    }

    @PostLoad
    @PostPersist
    void marcarComoExistente() {
        this.novo = false;
    }

    @Override
    public UUID getId()                 { return id; }

    Long getPedidoId()                  { return pedidoId; }
    Long getUsuarioId()                 { return usuarioId; }
    BigDecimal getValor()               { return valor; }
    String getMoeda()                   { return moeda; }
    FormaPagamento getForma()           { return forma; }
    StatusPagamento getStatus()         { return status; }
    MotivoRecusa getMotivo()            { return motivo; }
    String getCartaoMascarado()         { return cartaoMascarado; }
    String getCodigoAutorizacao()       { return codigoAutorizacao; }
    LocalDateTime getCriadoEm()         { return criadoEm; }
    LocalDateTime getProcessadoEm()     { return processadoEm; }
}
