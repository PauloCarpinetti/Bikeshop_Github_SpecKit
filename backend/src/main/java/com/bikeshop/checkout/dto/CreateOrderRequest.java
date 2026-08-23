package com.bikeshop.checkout.dto;

import com.bikeshop.orders.EnderecoEntregaInput;
import com.bikeshop.payments.PaymentProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotBlank String clienteNome,
        @NotBlank @Email String clienteEmail,
        @NotNull @Valid EnderecoEntregaInput endereco,
        @NotNull PaymentProvider paymentProvider
) {
}
