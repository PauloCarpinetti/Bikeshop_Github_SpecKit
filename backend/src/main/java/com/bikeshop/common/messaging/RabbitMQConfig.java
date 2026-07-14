package com.bikeshop.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange único ("bikeshop.events") com uma fila por domínio, conforme decisão registrada em
 * research.md (seção 8): orders.events, inventory.events, notifications.events.
 */
@Configuration
public class RabbitMQConfig {

  public static final String EXCHANGE = "bikeshop.events";

  public static final String ORDERS_QUEUE = "orders.events";
  public static final String INVENTORY_QUEUE = "inventory.events";
  public static final String NOTIFICATIONS_QUEUE = "notifications.events";

  @Bean
  public TopicExchange bikeshopExchange() {
    return new TopicExchange(EXCHANGE);
  }

  @Bean
  public Queue ordersQueue() {
    return new Queue(ORDERS_QUEUE, true);
  }

  @Bean
  public Queue inventoryQueue() {
    return new Queue(INVENTORY_QUEUE, true);
  }

  @Bean
  public Queue notificationsQueue() {
    return new Queue(NOTIFICATIONS_QUEUE, true);
  }

  @Bean
  public Binding ordersBinding(Queue ordersQueue, TopicExchange bikeshopExchange) {
    return BindingBuilder.bind(ordersQueue).to(bikeshopExchange).with("orders.*");
  }

  @Bean
  public Binding inventoryBinding(Queue inventoryQueue, TopicExchange bikeshopExchange) {
    return BindingBuilder.bind(inventoryQueue).to(bikeshopExchange).with("inventory.*");
  }

  @Bean
  public Binding notificationsBinding(Queue notificationsQueue, TopicExchange bikeshopExchange) {
    return BindingBuilder.bind(notificationsQueue).to(bikeshopExchange).with("notifications.*");
  }

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}
