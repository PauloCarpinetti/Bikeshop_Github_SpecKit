package com.bikeshop.orders;

import com.bikeshop.audit.AuditService;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.exception.NotFoundException;
import com.bikeshop.orders.dto.OrderDto;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitação de troca/devolução de um pedido entregue (US2, FR-008). A transição de status é
 * feita por {@link OrderService#atualizarStatus} (que já notifica o cliente via T066); esta classe
 * garante a propriedade do pedido, a elegibilidade (só pedidos entregues) e registra o motivo no
 * Log de Auditoria (FR-011).
 */
@Service
@Transactional
public class ReturnService {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AuditService auditService;

    public ReturnService(OrderRepository orderRepository, OrderService orderService, AuditService auditService) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.auditService = auditService;
    }

    public OrderDto solicitar(Long clienteId, Long pedidoId, String motivo) {
        Pedido pedido = orderRepository.findByIdAndClienteId(pedidoId, clienteId)
                .orElseThrow(() -> new NotFoundException("Pedido", pedidoId));

        if (pedido.getStatus() != PedidoStatus.ENTREGUE) {
            throw new BusinessException("PEDIDO_NAO_ELEGIVEL_PARA_DEVOLUCAO",
                    "Só é possível solicitar troca/devolução de pedidos já entregues", HttpStatus.CONFLICT);
        }

        PedidoStatus statusAnterior = pedido.getStatus();
        Pedido atualizado = orderService.atualizarStatus(pedidoId, PedidoStatus.EM_TROCA_DEVOLUCAO);

        auditService.record("SOLICITAR_TROCA_DEVOLUCAO", "Pedido", String.valueOf(pedidoId),
                Map.of("status", statusAnterior.name()),
                Map.of("status", atualizado.getStatus().name(), "motivo", motivo));

        return orderService.toDto(atualizado);
    }
}
