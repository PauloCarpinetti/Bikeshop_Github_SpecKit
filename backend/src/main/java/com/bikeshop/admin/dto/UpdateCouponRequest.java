package com.bikeshop.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record UpdateCouponRequest(
        @NotBlank String tipo,
        @NotNull @DecimalMin("0.01") BigDecimal valor,
        @NotNull Instant validoDe,
        @NotNull Instant validoAte,
        BigDecimal valorMinimoCarrinho,
        List<String> categoriasAplicaveis,
        @Positive Integer limiteDeUso
) {
}
