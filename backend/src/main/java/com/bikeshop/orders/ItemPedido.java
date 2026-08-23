package com.bikeshop.orders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Item de pedido (spec.md, Key Entities): preço congelado no momento da compra, independente de
 * mudanças futuras no preço da variação de produto.
 */
@Entity
@Table(name = "item_pedido")
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(name = "variacao_produto_id", nullable = false)
    private Long variacaoProdutoId;

    @Column(nullable = false)
    private String sku;

    @Column(name = "nome_produto", nullable = false)
    private String nomeProduto;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    @Column(nullable = false)
    private int quantidade;

    protected ItemPedido() {
        // JPA
    }

    public ItemPedido(Pedido pedido, Long variacaoProdutoId, String sku, String nomeProduto,
                       BigDecimal precoUnitario, int quantidade) {
        this.pedido = pedido;
        this.variacaoProdutoId = variacaoProdutoId;
        this.sku = sku;
        this.nomeProduto = nomeProduto;
        this.precoUnitario = precoUnitario;
        this.quantidade = quantidade;
    }

    public Long getId() {
        return id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public Long getVariacaoProdutoId() {
        return variacaoProdutoId;
    }

    public String getSku() {
        return sku;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
