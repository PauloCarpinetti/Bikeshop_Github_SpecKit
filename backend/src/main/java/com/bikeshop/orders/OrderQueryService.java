package com.bikeshop.orders;

import com.bikeshop.common.exception.NotFoundException;
import com.bikeshop.orders.dto.OrderDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Histórico e detalhe de pedidos do cliente autenticado (FR-008, T056).
 */
@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public OrderQueryService(OrderRepository orderRepository, OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public List<OrderDto> listarPorCliente(Long clienteId) {
        return orderRepository.findByClienteIdOrderByCriadoEmDesc(clienteId).stream()
                .map(orderService::toDto)
                .toList();
    }

    public OrderDto detalharPorCliente(Long clienteId, Long orderId) {
        Pedido pedido = orderRepository.findByIdAndClienteId(orderId, clienteId)
                .orElseThrow(() -> new NotFoundException("Pedido", orderId));
        return orderService.toDto(pedido);
    }
}
