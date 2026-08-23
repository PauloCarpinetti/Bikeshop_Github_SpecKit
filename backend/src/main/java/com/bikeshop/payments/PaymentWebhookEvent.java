package com.bikeshop.payments;

public record PaymentWebhookEvent(
        String reference,
        String status
) {
}
