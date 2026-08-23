-- Fase 3C: Cliente (autenticação) e vínculo do pedido com o cliente autenticado.

CREATE TABLE cliente (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome         VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL,
    senha_hash   VARCHAR(255) NOT NULL,
    role         VARCHAR(32)  NOT NULL DEFAULT 'CUSTOMER',
    criado_em    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_cliente_email (email)
) ENGINE = InnoDB;

ALTER TABLE pedido
    ADD CONSTRAINT fk_pedido_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id);
