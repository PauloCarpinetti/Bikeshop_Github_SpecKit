package com.bikeshop.notifications;

import com.bikeshop.common.messaging.RabbitMQConfig;
import com.bikeshop.orders.OrderCreatedEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consome {@link OrderCreatedEvent} de {@code orders.events} e envia a confirmação de pedido por
 * e-mail (FR-007). Estabelece a base do módulo de notificações (T046b); a notificação de mudança
 * de status (T066, Fase 4) estende este mesmo módulo. Sem {@code SENDGRID_API_KEY} configurada
 * (ambiente atual), o envio é simulado — mesmo padrão de resiliência usado para frete/pagamento.
 */
@Component
public class OrderConfirmationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationListener.class);

    private final String sendgridApiKey;
    private final String fromEmail;
    private final RestClient restClient;

    public OrderConfirmationListener(@Value("${bikeshop.notifications.sendgrid.api-key:}") String sendgridApiKey,
                                      @Value("${bikeshop.notifications.sendgrid.from-email:pedidos@bikeshop.example}") String fromEmail) {
        this.sendgridApiKey = sendgridApiKey;
        this.fromEmail = fromEmail;
        this.restClient = RestClient.create("https://api.sendgrid.com");
    }

    @RabbitListener(queues = RabbitMQConfig.ORDERS_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
            log.warn("SendGrid sem credenciais configuradas — simulando envio de e-mail de confirmação "
                    + "do pedido #{} para {} ({})", event.pedidoId(), event.clienteNome(), event.clienteEmail());
            return;
        }

        try {
            SendGridEmail to = new SendGridEmail(event.clienteEmail(), event.clienteNome());
            SendGridEmail from = new SendGridEmail(fromEmail, "BikeShop");
            String text = "Olá %s, recebemos seu pedido #%d! Em breve você receberá atualizações sobre o envio."
                    .formatted(event.clienteNome(), event.pedidoId());
            SendGridMessage body = new SendGridMessage(
                    List.of(new SendGridPersonalization(List.of(to))),
                    from,
                    "Confirmação do pedido #%d".formatted(event.pedidoId()),
                    List.of(new SendGridContent("text/plain", text))
            );

            restClient.post()
                    .uri("/v3/mail/send")
                    .header("Authorization", "Bearer " + sendgridApiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("E-mail de confirmação do pedido #{} enviado para {}", event.pedidoId(), event.clienteEmail());
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail de confirmação via SendGrid para o pedido #{}: {}",
                    event.pedidoId(), ex.getMessage());
        }
    }

    private record SendGridEmail(String email, String name) {
    }

    private record SendGridContent(String type, String value) {
    }

    private record SendGridPersonalization(List<SendGridEmail> to) {
    }

    private record SendGridMessage(List<SendGridPersonalization> personalizations, SendGridEmail from,
                                    String subject, List<SendGridContent> content) {
    }
}
