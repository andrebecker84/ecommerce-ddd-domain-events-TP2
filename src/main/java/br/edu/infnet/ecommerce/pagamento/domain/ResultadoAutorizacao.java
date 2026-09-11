package br.edu.infnet.ecommerce.pagamento.domain;

/** Resposta da porta {@link ProcessadorCartao}. */
public record ResultadoAutorizacao(boolean autorizado,
                                   String codigoAutorizacao,
                                   MotivoRecusa motivo) {

    public static ResultadoAutorizacao autorizado(String codigoAutorizacao) {
        return new ResultadoAutorizacao(true, codigoAutorizacao, null);
    }

    public static ResultadoAutorizacao negado(MotivoRecusa motivo) {
        return new ResultadoAutorizacao(false, null, motivo);
    }
}
