package com.bikeshop.customers;

import com.bikeshop.orders.OrderQueryService;
import com.bikeshop.orders.dto.OrderDto;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Histórico e detalhe de pedidos do cliente autenticado (FR-008, T057).
 */
@RestController
@RequestMapping("/api/v1/account/orders")
public class AccountOrdersController {

    private final OrderQueryService orderQueryService;

    public AccountOrdersController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping
    public List<OrderDto> listOrders(Authentication authentication) {
        return orderQueryService.listarPorCliente(clienteId(authentication));
    }

    @GetMapping("/{orderId}")
    public OrderDto getOrder(Authentication authentication, @PathVariable Long orderId) {
        return orderQueryService.detalharPorCliente(clienteId(authentication), orderId);
    }

    private Long clienteId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
