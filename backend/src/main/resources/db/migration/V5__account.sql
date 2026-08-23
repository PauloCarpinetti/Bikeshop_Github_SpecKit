-- Fase 4A: dados de perfil estendido do cliente e endereços salvos.

ALTER TABLE cliente ADD COLUMN telefone VARCHAR(20) NULL;

CREATE TABLE endereco (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id   BIGINT       NOT NULL,
    cep          VARCHAR(9)   NOT NULL,
    logradouro   VARCHAR(255) NOT NULL,
    numero       VARCHAR(20)  NOT NULL,
    complemento  VARCHAR(255),
    bairro       VARCHAR(255) NOT NULL,
    cidade       VARCHAR(255) NOT NULL,
    estado       VARCHAR(2)   NOT NULL,
    tipo         VARCHAR(20)  NOT NULL DEFAULT 'ENTREGA',
    padrao       BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_endereco_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
) ENGINE = InnoDB;
