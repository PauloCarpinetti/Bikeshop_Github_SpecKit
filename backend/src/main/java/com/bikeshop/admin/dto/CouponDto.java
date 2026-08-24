package com.bikeshop.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CouponDto(
        Long id,
        String codigo,
        String tipo,
        BigDecimal valor,
        Instant validoDe,
        Instant validoAte,
        BigDecimal valorMinimoCarrinho,
        List<String> categoriasAplicaveis,
        Integer limiteDeUso,
        int usosRealizados
) {
}
