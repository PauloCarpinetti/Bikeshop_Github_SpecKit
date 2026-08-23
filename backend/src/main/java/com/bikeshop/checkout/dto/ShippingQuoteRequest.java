package com.bikeshop.checkout.dto;

import jakarta.validation.constraints.NotBlank;

public record ShippingQuoteRequest(
        @NotBlank String cep
) {
}
