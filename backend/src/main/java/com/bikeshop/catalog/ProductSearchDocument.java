package com.bikeshop.catalog;

import java.math.BigDecimal;
import java.util.List;

/**
 * Documento denormalizado indexado no Meilisearch (índice "products"). O detalhe completo do
 * produto continua vindo do MySQL; este documento serve apenas para busca/facetas (FR-003).
 */
public class ProductSearchDocument {

    private String id;
    private String slug;
    private String nome;
    private String descricao;
    private String categoria;
    private String marca;
    private String modalidade;
    private BigDecimal precoMinimo;
    private BigDecimal precoMaximo;
    private List<String> tamanhos;
    private String imagemPrincipal;

    public ProductSearchDocument() {
        // Jackson
    }

    public ProductSearchDocument(String id, String slug, String nome, String descricao, String categoria,
                                  String marca, String modalidade, BigDecimal precoMinimo, BigDecimal precoMaximo,
                                  List<String> tamanhos, String imagemPrincipal) {
        this.id = id;
        this.slug = slug;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.marca = marca;
        this.modalidade = modalidade;
        this.precoMinimo = precoMinimo;
        this.precoMaximo = precoMaximo;
        this.tamanhos = tamanhos;
        this.imagemPrincipal = imagemPrincipal;
    }

    public String getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getMarca() {
        return marca;
    }

    public String getModalidade() {
        return modalidade;
    }

    public BigDecimal getPrecoMinimo() {
        return precoMinimo;
    }

    public BigDecimal getPrecoMaximo() {
        return precoMaximo;
    }

    public List<String> getTamanhos() {
        return tamanhos;
    }

    public String getImagemPrincipal() {
        return imagemPrincipal;
    }
}
