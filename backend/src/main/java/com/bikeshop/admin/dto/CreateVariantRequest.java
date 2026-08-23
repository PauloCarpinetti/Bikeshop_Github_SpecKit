package com.bikeshop.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Map;

public record CreateVariantRequest(
        @NotBlank String sku,
        @NotNull Map<String, Object> atributos,
        @NotNull @DecimalMin("0.01") BigDecimal preco,
        @PositiveOrZero int estoqueDisponivel,
        @NotNull @DecimalMin("0.001") BigDecimal pesoKg,
        @NotNull @DecimalMin("0.01") BigDecimal alturaCm,
        @NotNull @DecimalMin("0.01") BigDecimal larguraCm,
        @NotNull @DecimalMin("0.01") BigDecimal comprimentoCm
) {
}
