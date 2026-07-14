package com.bikeshop.cart;

import java.io.Serializable;

/**
 * Um item de carrinho por SKU (variação de produto). O {@code variacaoProdutoId} funciona também
 * como identificador da linha do carrinho nos endpoints REST (não há duplicidade de SKU no mesmo
 * carrinho — adicionar novamente soma a quantidade).
 */
public class ItemCarrinho implements Serializable {

    private Long variacaoProdutoId;
    private int quantidade;

    public ItemCarrinho() {
        // Jackson (Redis)
    }

    public ItemCarrinho(Long variacaoProdutoId, int quantidade) {
        this.variacaoProdutoId = variacaoProdutoId;
        this.quantidade = quantidade;
    }

    public Long getVariacaoProdutoId() {
        return variacaoProdutoId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}
