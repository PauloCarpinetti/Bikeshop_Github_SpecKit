-- Fase 3B: dimensões/peso para cálculo de frete (FR-005) e Pedido/ItemPedido (FR-006, FR-007).

ALTER TABLE variacao_produto
    ADD COLUMN peso_kg        DECIMAL(6,3)  NOT NULL DEFAULT 1.000,
    ADD COLUMN altura_cm      DECIMAL(6,2)  NOT NULL DEFAULT 15.00,
    ADD COLUMN largura_cm     DECIMAL(6,2)  NOT NULL DEFAULT 30.00,
    ADD COLUMN comprimento_cm DECIMAL(6,2)  NOT NULL DEFAULT 90.00;

CREATE TABLE pedido (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id             VARCHAR(64) NULL,
    cliente_id          BIGINT NULL,
    cliente_nome        VARCHAR(255) NOT NULL,
    cliente_email       VARCHAR(255) NOT NULL,
    endereco_entrega    JSON NOT NULL,
    valor_itens         DECIMAL(10,2) NOT NULL,
    valor_frete         DECIMAL(10,2) NOT NULL,
    transportadora      VARCHAR(64) NULL,
    prazo_frete_dias    INT NULL,
    valor_total         DECIMAL(10,2) NOT NULL,
    cupom_codigo        VARCHAR(64) NULL,
    status              VARCHAR(32) NOT NULL,
    status_historico    JSON NOT NULL,
    payment_provider    VARCHAR(32) NULL,
    payment_reference   VARCHAR(128) NULL,
    payment_status      VARCHAR(32) NULL,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_pedido_cliente_id (cliente_id),
    INDEX idx_pedido_status (status),
    INDEX idx_pedido_payment_reference (payment_reference)
) ENGINE = InnoDB;

CREATE TABLE item_pedido (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id             BIGINT NOT NULL,
    variacao_produto_id   BIGINT NOT NULL,
    sku                   VARCHAR(64) NOT NULL,
    nome_produto          VARCHAR(255) NOT NULL,
    preco_unitario        DECIMAL(10,2) NOT NULL,
    quantidade            INT NOT NULL,

    INDEX idx_item_pedido_pedido_id (pedido_id),
    CONSTRAINT fk_item_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedido (id)
) ENGINE = InnoDB;
