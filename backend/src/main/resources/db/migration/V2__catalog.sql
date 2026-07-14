-- Fase 3A: catálogo (Produto + Variação de Produto).

CREATE TABLE produto (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                    VARCHAR(255) NOT NULL,
    slug                    VARCHAR(255) NOT NULL,
    descricao               TEXT NULL,
    categoria               VARCHAR(100) NOT NULL,
    marca                   VARCHAR(100) NULL,
    modalidade              VARCHAR(100) NULL,
    especificacoes_tecnicas JSON NULL,
    tabela_geometria        JSON NULL,
    imagens                 JSON NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_produto_slug (slug),
    INDEX idx_produto_categoria (categoria),
    INDEX idx_produto_marca (marca)
) ENGINE = InnoDB;

CREATE TABLE variacao_produto (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id          BIGINT NOT NULL,
    sku                 VARCHAR(64) NOT NULL,
    atributos           JSON NULL,
    preco               DECIMAL(10,2) NOT NULL,
    estoque_disponivel  INT NOT NULL DEFAULT 0,
    estoque_reservado   INT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'DISPONIVEL',
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_variacao_sku (sku),
    INDEX idx_variacao_produto_id (produto_id),
    CONSTRAINT fk_variacao_produto FOREIGN KEY (produto_id) REFERENCES produto (id)
) ENGINE = InnoDB;
