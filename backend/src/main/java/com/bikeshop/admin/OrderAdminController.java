package com.bikeshop.admin;

import com.bikeshop.admin.dto.UpdateOrderStatusRequest;
import com.bikeshop.orders.dto.OrderDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestão de pedidos no backoffice (FR-007, FR-009, T076). Protegido por
 * {@code hasAnyRole("OPERATOR", "ADMIN")} em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;

    public OrderAdminController(OrderAdminService orderAdminService) {
        this.orderAdminService = orderAdminService;
    }

    @GetMapping
    public List<OrderDto> listar() {
        return orderAdminService.listar();
    }

    @PatchMapping("/{id}")
    public OrderDto atualizarStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderAdminService.atualizarStatus(id, request.status());
    }
}
