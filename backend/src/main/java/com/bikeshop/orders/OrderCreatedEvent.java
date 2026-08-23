package com.bikeshop.orders;

/**
 * Evento publicado em {@code orders.events} (routing key {@code orders.created}) quando um pedido
 * é criado no checkout (FR-007, T046). Consumido por {@code OrderConfirmationListener} (T046b).
 */
public record OrderCreatedEvent(
        Long pedidoId,
        String clienteNome,
        String clienteEmail
) {
}
