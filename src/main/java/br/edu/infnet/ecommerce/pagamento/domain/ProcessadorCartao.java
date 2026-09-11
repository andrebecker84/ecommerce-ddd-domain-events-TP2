package br.edu.infnet.ecommerce.pagamento.domain;

/**
 * Porta de saída: o domínio declara o que precisa, não como é feito.
 *
 * Substitui a dependência direta da classe concreta {@code ProcessadorPagamento}
 * que existia no monólito. Trocar o provedor real ou usar um duplo de teste
 * passa a ser uma decisão de infraestrutura.
 */
public interface ProcessadorCartao {

    ResultadoAutorizacao autorizar(NumeroCartao cartao, Dinheiro valor);
}
