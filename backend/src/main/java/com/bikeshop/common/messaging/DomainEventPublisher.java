package com.bikeshop.common.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher base de eventos de domínio. Módulos específicos (orders, admin/estoque, notifications)
 * publicam através deste componente usando uma routing key própria (ex.: "orders.created",
 * "inventory.adjusted"), roteada pelo RabbitMQConfig para a fila correspondente.
 */
@Component
public class DomainEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  public DomainEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publish(String routingKey, Object payload) {
    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, routingKey, payload);
  }
}
