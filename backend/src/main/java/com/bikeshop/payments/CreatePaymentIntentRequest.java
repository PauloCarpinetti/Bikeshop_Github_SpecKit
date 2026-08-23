package com.bikeshop.payments;

import jakarta.validation.constraints.NotNull;

public record CreatePaymentIntentRequest(
        @NotNull PaymentProvider provider
) {
}
