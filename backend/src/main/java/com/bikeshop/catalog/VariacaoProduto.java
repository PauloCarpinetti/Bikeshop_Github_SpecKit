package com.bikeshop.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Variação de Produto (spec.md, Key Entities): combinação específica de atributos
 * (tamanho, cor, material) com SKU, preço e estoque próprios (FR-001).
 */
@Entity
@Table(name = "variacao_produto")
public class VariacaoProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false, unique = true)
    private String sku;

    @JdbcTypeCode(SqlTypes.JSON)
    private String atributos;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "estoque_disponivel", nullable = false)
    private int estoqueDisponivel;

    @Column(name = "estoque_reservado", nullable = false)
    private int estoqueReservado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VariacaoStatus status = VariacaoStatus.DISPONIVEL;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected VariacaoProduto() {
        // JPA
    }

    public VariacaoProduto(Produto produto, String sku, String atributos, BigDecimal preco, int estoqueDisponivel) {
        this.produto = produto;
        this.sku = sku;
        this.atributos = atributos;
        this.preco = preco;
        this.estoqueDisponivel = estoqueDisponivel;
        this.estoqueReservado = 0;
        this.status = estoqueDisponivel > 0 ? VariacaoStatus.DISPONIVEL : VariacaoStatus.ESGOTADO;
    }

    public Long getId() {
        return id;
    }

    public Produto getProduto() {
        return produto;
    }

    public String getSku() {
        return sku;
    }

    public String getAtributos() {
        return atributos;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public int getEstoqueDisponivel() {
        return estoqueDisponivel;
    }

    public int getEstoqueReservado() {
        return estoqueReservado;
    }

    public VariacaoStatus getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
