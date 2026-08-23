-- Fase 4B: avaliações de produto (publicadas por cliente para pedido entregue).

CREATE TABLE avaliacao (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id   BIGINT       NOT NULL,
    cliente_id   BIGINT       NOT NULL,
    pedido_id    BIGINT       NOT NULL,
    nota         INT          NOT NULL,
    comentario   VARCHAR(2000),
    status       VARCHAR(20)  NOT NULL DEFAULT 'PUBLICADA',
    criado_em    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_avaliacao_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT fk_avaliacao_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_avaliacao_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id)
) ENGINE = InnoDB;
