package com.bikeshop.notifications;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Ponto único de envio de e-mail transacional via SendGrid, usado pelos consumidores do módulo de
 * notificações. Sem {@code SENDGRID_API_KEY} configurada (ambiente atual), o envio é simulado —
 * mesmo padrão de resiliência usado para frete/pagamento.
 */
@Component
public class SendGridEmailSender {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailSender.class);

    private final String sendgridApiKey;
    private final String fromEmail;
    private final RestClient restClient;

    public SendGridEmailSender(@Value("${bikeshop.notifications.sendgrid.api-key:}") String sendgridApiKey,
                                @Value("${bikeshop.notifications.sendgrid.from-email:pedidos@bikeshop.example}") String fromEmail) {
        this.sendgridApiKey = sendgridApiKey;
        this.fromEmail = fromEmail;
        this.restClient = RestClient.create("https://api.sendgrid.com");
    }

    public void send(String toEmail, String toName, String subject, String text) {
        if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
            log.warn("SendGrid sem credenciais configuradas — simulando envio de e-mail \"{}\" para {} ({})",
                    subject, toName, toEmail);
            return;
        }

        try {
            SendGridEmail to = new SendGridEmail(toEmail, toName);
            SendGridEmail from = new SendGridEmail(fromEmail, "BikeShop");
            SendGridMessage body = new SendGridMessage(
                    List.of(new SendGridPersonalization(List.of(to))),
                    from,
                    subject,
                    List.of(new SendGridContent("text/plain", text))
            );

            restClient.post()
                    .uri("/v3/mail/send")
                    .header("Authorization", "Bearer " + sendgridApiKey)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("E-mail \"{}\" enviado para {}", subject, toEmail);
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail via SendGrid para {}: {}", toEmail, ex.getMessage());
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
