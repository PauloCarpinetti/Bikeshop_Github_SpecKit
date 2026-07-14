package com.bikeshop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test da Fase 2 (Foundational): garante que o contexto Spring sobe com segurança,
 * JPA/Flyway, Redis, RabbitMQ e OpenAPI configurados.
 */
@SpringBootTest
@ActiveProfiles("test")
class BikeshopApplicationTests {

  @Test
  void contextLoads() {}
}
