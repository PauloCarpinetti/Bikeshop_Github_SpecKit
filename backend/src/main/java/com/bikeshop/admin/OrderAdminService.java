package com.bikeshop.admin;

import com.bikeshop.audit.AuditService;
import com.bikeshop.orders.OrderRepository;
import com.bikeshop.orders.OrderService;
import com.bikeshop.orders.Pedido;
import com.bikeshop.orders.PedidoStatus;
import com.bikeshop.orders.dto.OrderDto;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de pedidos no backoffice (FR-007, FR-009, T076): listagem e atualização manual de
 * status. Reaproveita {@link OrderService#atualizarStatus} (já valida a transição e notifica o
 * cliente — T066) e registra a ação no Log de Auditoria (FR-011), já que é uma intervenção
 * administrativa, diferente da transição automática do checkout/webhook de pagamento.
 */
@Service
@Transactional
public class OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuditService auditService;

    public OrderAdminService(OrderRepository orderRepository, OrderService orderService, AuditService auditService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> listar() {
        return orderRepository.findAll(Sort.by(Sort.Direction.DESC, "criadoEm")).stream()
                .map(orderService::toDto)
                .toList();
    }

    public OrderDto atualizarStatus(Long pedidoId, String novoStatusValor) {
        Pedido pedido = orderService.buscarPorId(pedidoId);
        PedidoStatus statusAnterior = pedido.getStatus();
        PedidoStatus novoStatus = PedidoStatus.valueOf(novoStatusValor);

        Pedido atualizado = orderService.atualizarStatus(pedidoId, novoStatus);

        auditService.record("ATUALIZAR_STATUS_PEDIDO", "Pedido", String.valueOf(pedidoId),
                Map.of("status", statusAnterior.name()), Map.of("status", novoStatus.name()));

        return orderService.toDto(atualizado);
    }
}
