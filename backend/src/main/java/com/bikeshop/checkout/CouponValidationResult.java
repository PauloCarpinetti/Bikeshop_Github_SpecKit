package com.bikeshop.checkout;

import com.bikeshop.admin.CupomDesconto;
import java.math.BigDecimal;

public record CouponValidationResult(CupomDesconto cupom, BigDecimal valorDesconto) {
}
