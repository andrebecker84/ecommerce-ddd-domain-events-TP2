package br.edu.infnet.ecommerce.pagamento.domain;

/**
 * Formas suportadas pelo contexto. Hoje só existe cartão. PIX e boleto
 * entram aqui sem que nenhum outro contexto precise saber.
 *
 * {@code NAO_SUPORTADA} existe para que uma forma desconhecida vinda da
 * fronteira vire uma recusa de negócio avaliada pelo agregado, e não uma
 * exceção técnica lançada antes de o pagamento sequer existir.
 */
public enum FormaPagamento {

    CARTAO,
    NAO_SUPORTADA;

    public static FormaPagamento reconhecer(String valor) {
        if (valor != null && CARTAO.name().equalsIgnoreCase(valor.trim())) {
            return CARTAO;
        }
        return NAO_SUPORTADA;
    }
}
