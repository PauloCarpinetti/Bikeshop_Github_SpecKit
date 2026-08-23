package com.bikeshop.notifications;

import com.bikeshop.common.messaging.RabbitMQConfig;
import com.bikeshop.orders.OrderCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome {@link OrderCreatedEvent} de {@code orders.events} e envia a confirmação de pedido por
 * e-mail (FR-007). Estabelece a base do módulo de notificações (T046b); a notificação de mudança
 * de status ({@link OrderStatusChangedListener}, T066, Fase 4) reaproveita o mesmo
 * {@link SendGridEmailSender}.
 */
@Component
public class OrderConfirmationListener {

    private final SendGridEmailSender emailSender;

    public OrderConfirmationListener(SendGridEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDERS_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        String text = "Olá %s, recebemos seu pedido #%d! Em breve você receberá atualizações sobre o envio."
                .formatted(event.clienteNome(), event.pedidoId());
        emailSender.send(event.clienteEmail(), event.clienteNome(),
                "Confirmação do pedido #%d".formatted(event.pedidoId()), text);
    }
}
