package com.bikeshop.orders;

public record OrderStatusChangedEvent(
        Long pedidoId, String clienteNome, String clienteEmail, String statusAnterior, String novoStatus
) {
}
