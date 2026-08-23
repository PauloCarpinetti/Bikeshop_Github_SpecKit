package com.bikeshop.notifications;

import com.bikeshop.common.messaging.RabbitMQConfig;
import com.bikeshop.orders.OrderStatusChangedEvent;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@link OrderStatusChangedEvent} de {@code orders.status.events} e notifica o cliente
 * sobre a mudança de status do pedido (FR-008, T066). Reaproveita {@link SendGridEmailSender}.
 */
@Component
public class OrderStatusChangedListener {

    private static final Map<String, String> STATUS_LABELS = Map.of(
            "PAGO", "pagamento confirmado",
            "EM_SEPARACAO", "em separação",
            "ENVIADO", "enviado",
            "ENTREGUE", "entregue",
            "PAGAMENTO_RECUSADO", "pagamento recusado",
            "CANCELADO", "cancelado",
            "EM_TROCA_DEVOLUCAO", "em troca/devolução"
    );

    private final SendGridEmailSender emailSender;

    public OrderStatusChangedListener(SendGridEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDERS_STATUS_QUEUE)
    public void onStatusChanged(OrderStatusChangedEvent event) {
        String statusLegivel = STATUS_LABELS.getOrDefault(event.novoStatus(), event.novoStatus());
        String text = "Olá %s, o status do seu pedido #%d agora é: %s."
                .formatted(event.clienteNome(), event.pedidoId(), statusLegivel);
        emailSender.send(event.clienteEmail(), event.clienteNome(),
                "Atualização do pedido #%d".formatted(event.pedidoId()), text);
    }
}
