package com.bikeshop.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

public record UpdateVariantRequest(
        @NotNull Map<String, Object> atributos,
        @NotNull @DecimalMin("0.01") BigDecimal preco,
        @NotBlank String status,
        @NotNull @DecimalMin("0.001") BigDecimal pesoKg,
        @NotNull @DecimalMin("0.01") BigDecimal alturaCm,
        @NotNull @DecimalMin("0.01") BigDecimal larguraCm,
        @NotNull @DecimalMin("0.01") BigDecimal comprimentoCm
) {
}
