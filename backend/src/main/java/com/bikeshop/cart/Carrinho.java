package com.bikeshop.cart;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Carrinho (spec.md, Key Entities). Nesta sub-fase (3A) representa apenas o carrinho de visitante,
 * persistido no Redis com TTL (research.md, seção 3). A persistência em MySQL para carrinho
 * autenticado e o merge visitante->cliente entram na sub-fase 3C, junto com a autenticação.
 */
public class Carrinho implements Serializable {

    private String id;
    private List<ItemCarrinho> itens = new ArrayList<>();
    private Instant criadoEm;
    private Instant atualizadoEm;

    public Carrinho() {
        // Jackson (Redis)
    }

    public Carrinho(String id) {
        this.id = id;
        this.criadoEm = Instant.now();
        this.atualizadoEm = Instant.now();
    }

    public String getId() {
        return id;
    }

    public List<ItemCarrinho> getItens() {
        return itens;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Optional<ItemCarrinho> findItem(Long variacaoProdutoId) {
        return itens.stream().filter(item -> item.getVariacaoProdutoId().equals(variacaoProdutoId)).findFirst();
    }

    public void upsertItem(Long variacaoProdutoId, int quantidade) {
        Optional<ItemCarrinho> existente = findItem(variacaoProdutoId);
        if (existente.isPresent()) {
            existente.get().setQuantidade(quantidade);
        } else {
            itens.add(new ItemCarrinho(variacaoProdutoId, quantidade));
        }
        this.atualizadoEm = Instant.now();
    }

    public void removeItem(Long variacaoProdutoId) {
        itens.removeIf(item -> item.getVariacaoProdutoId().equals(variacaoProdutoId));
        this.atualizadoEm = Instant.now();
    }
}
