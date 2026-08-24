package com.bikeshop.orders.dto;

import com.bikeshop.orders.EnderecoEntregaInput;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
        Long id,
        String clienteNome,
        String clienteEmail,
        String status,
        BigDecimal valorItens,
        BigDecimal valorFrete,
        BigDecimal valorDesconto,
        String cupomCodigo,
        BigDecimal valorTotal,
        String transportadora,
        Integer prazoFreteDias,
        String paymentProvider,
        String paymentReference,
        String paymentStatus,
        Instant criadoEm,
        List<OrderStatusHistoryEntryDto> statusHistorico,
        EnderecoEntregaInput enderecoEntrega,
        List<OrderItemDto> itens
) {
}
