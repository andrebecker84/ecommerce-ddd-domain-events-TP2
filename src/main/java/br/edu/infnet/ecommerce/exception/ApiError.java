package br.edu.infnet.ecommerce.exception;

import java.time.LocalDateTime;

public record ApiError(
        int status,
        String erro,
        String mensagem,
        LocalDateTime instante
) {
}
