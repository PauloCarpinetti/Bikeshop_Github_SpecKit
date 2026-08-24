-- Fase 5B: cupons de desconto (backoffice) e valor de desconto aplicado no pedido.

CREATE TABLE cupom_desconto (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo                  VARCHAR(50)  NOT NULL,
    tipo                    VARCHAR(20)  NOT NULL,
    valor                   DECIMAL(10,2) NOT NULL,
    valido_de               TIMESTAMP    NOT NULL,
    valido_ate              TIMESTAMP    NOT NULL,
    valor_minimo_carrinho   DECIMAL(10,2),
    categorias_aplicaveis   JSON,
    limite_de_uso           INT,
    usos_realizados         INT          NOT NULL DEFAULT 0,
    criado_em               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_cupom_codigo (codigo)
) ENGINE = InnoDB;

ALTER TABLE pedido ADD COLUMN valor_desconto DECIMAL(10,2) NOT NULL DEFAULT 0;
