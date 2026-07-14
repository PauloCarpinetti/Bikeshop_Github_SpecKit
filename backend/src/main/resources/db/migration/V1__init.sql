-- Baseline schema: infraestrutura compartilhada (Fase 2 - Foundational).
-- Tabelas de domínio (produto, pedido, cliente, etc.) são adicionadas em migrações
-- subsequentes (V2+) conforme cada user story é implementada.

CREATE TABLE audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor           VARCHAR(255) NOT NULL,
    actor_role      VARCHAR(64)  NOT NULL,
    action          VARCHAR(128) NOT NULL,
    entity_name     VARCHAR(128) NOT NULL,
    entity_id       VARCHAR(64)  NOT NULL,
    previous_state  JSON         NULL,
    new_state       JSON         NULL,
    origin          VARCHAR(255) NULL,
    occurred_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_audit_log_entity (entity_name, entity_id),
    INDEX idx_audit_log_occurred_at (occurred_at)
) ENGINE = InnoDB;
