package br.edu.infnet.ecommerce.pagamento.domain;

import java.util.Optional;

/**
 * Objeto de Valor que valida o formato do cartão e nunca revela o número cru.
 *
 * O {@code toString()} devolve a versão mascarada de propósito: um log
 * acidental do objeto não expõe o dado sensível.
 */
public record NumeroCartao(String valor) {

    private static final String FORMATO = "\\d{13,19}";

    public NumeroCartao {
        if (valor == null || !valor.matches(FORMATO)) {
            throw new IllegalArgumentException("Número de cartão inválido");
        }
    }

    /**
     * Fábrica que não lança: devolve vazio quando o número não tem formato
     * de cartão. Permite que a fronteira trate cartão inválido como recusa
     * de negócio, e não como erro técnico.
     */
    public static Optional<NumeroCartao> tentar(String valor) {
        return valor != null && valor.matches(FORMATO)
                ? Optional.of(new NumeroCartao(valor))
                : Optional.empty();
    }

    public String ultimosQuatro() {
        return valor.substring(valor.length() - 4);
    }

    public String mascarado() {
        return "**** **** **** " + ultimosQuatro();
    }

    @Override
    public String toString() {
        return mascarado();
    }
}
