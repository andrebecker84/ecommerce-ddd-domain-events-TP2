package br.edu.infnet.ecommerce.pagamento.infrastructure.persistence;

import br.edu.infnet.ecommerce.pagamento.domain.Dinheiro;
import br.edu.infnet.ecommerce.pagamento.domain.Pagamento;
import br.edu.infnet.ecommerce.pagamento.domain.PagamentoId;

import java.util.Currency;

/**
 * Tradução entre o agregado e a entidade JPA.
 *
 * É este arquivo, e só ele, que precisa mudar quando o esquema do banco
 * mudar. O modelo de domínio permanece intacto.
 */
final class PagamentoJpaMapper {

    private PagamentoJpaMapper() {
    }

    static PagamentoJpaEntity paraEntidade(Pagamento pagamento) {
        return new PagamentoJpaEntity(
                pagamento.id().valor(),
                pagamento.pedidoId(),
                pagamento.usuarioId(),
                pagamento.valor().valor(),
                pagamento.valor().moeda().getCurrencyCode(),
                pagamento.forma(),
                pagamento.status(),
                pagamento.motivo(),
                pagamento.cartaoMascarado(),
                pagamento.codigoAutorizacao(),
                pagamento.criadoEm(),
                pagamento.processadoEm());
    }

    static Pagamento paraDominio(PagamentoJpaEntity entidade) {
        return Pagamento.reconstituir(
                new PagamentoId(entidade.getId()),
                entidade.getPedidoId(),
                entidade.getUsuarioId(),
                new Dinheiro(entidade.getValor(), Currency.getInstance(entidade.getMoeda())),
                entidade.getForma(),
                entidade.getCartaoMascarado(),
                entidade.getStatus(),
                entidade.getMotivo(),
                entidade.getCodigoAutorizacao(),
                entidade.getCriadoEm(),
                entidade.getProcessadoEm());
    }
}
