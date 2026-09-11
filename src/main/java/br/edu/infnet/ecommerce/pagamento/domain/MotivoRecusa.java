package br.edu.infnet.ecommerce.pagamento.domain;

/**
 * Motivos de recusa do contexto. Os nomes são os mesmos que o monólito
 * devolvia em texto livre, para que a API pública não mude na migração.
 */
public enum MotivoRecusa {
    VALOR_INVALIDO,
    CARTAO_INVALIDO,
    FORMA_PAGAMENTO_NAO_SUPORTADA,
    LIMITE_EXCEDIDO,
    CARTAO_BLOQUEADO
}
