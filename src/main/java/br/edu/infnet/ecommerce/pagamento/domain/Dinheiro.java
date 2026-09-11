package br.edu.infnet.ecommerce.pagamento.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Objeto de Valor: imutável, sem identidade e autovalidado.
 *
 * Substitui o {@code BigDecimal} solto que o monólito usava, garantindo escala
 * fixa de duas casas e impedindo comparação entre moedas diferentes.
 */
public record Dinheiro(BigDecimal valor, Currency moeda) {

    private static final Currency BRL = Currency.getInstance("BRL");

    public Dinheiro {
        Objects.requireNonNull(valor, "valor é obrigatório");
        Objects.requireNonNull(moeda, "moeda é obrigatória");
        valor = valor.setScale(2, RoundingMode.HALF_UP);
    }

    public static Dinheiro reais(BigDecimal valor) {
        return new Dinheiro(valor, BRL);
    }

    public boolean ehPositivo() {
        return valor.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean maiorQue(Dinheiro outro) {
        exigirMesmaMoeda(outro);
        return valor.compareTo(outro.valor) > 0;
    }

    private void exigirMesmaMoeda(Dinheiro outro) {
        Objects.requireNonNull(outro, "comparação com valor nulo");
        if (!moeda.equals(outro.moeda)) {
            throw new IllegalArgumentException("Moedas diferentes não são comparáveis");
        }
    }

    @Override
    public String toString() {
        return moeda.getCurrencyCode() + " " + valor.toPlainString();
    }
}
