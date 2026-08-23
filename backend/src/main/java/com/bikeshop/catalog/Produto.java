package com.bikeshop.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Produto (spec.md, Key Entities): oferta comercial principal. Campos JSON (especificações,
 * geometria, imagens) são armazenados como texto JSON pré-serializado, no mesmo padrão já validado
 * em {@code AuditLog}, e desserializados na camada de mapeamento (ver {@link ProductMapper}).
 */
@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private String categoria;

    private String marca;

    private String modalidade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "especificacoes_tecnicas")
    private String especificacoesTecnicas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tabela_geometria")
    private String tabelaGeometria;

    @JdbcTypeCode(SqlTypes.JSON)
    private String imagens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProdutoStatus status = ProdutoStatus.ATIVO;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected Produto() {
        // JPA
    }

    public Produto(String nome, String slug, String descricao, String categoria, String marca,
                    String modalidade, String especificacoesTecnicas, String tabelaGeometria, String imagens) {
        this.nome = nome;
        this.slug = slug;
        this.descricao = descricao;
        this.categoria = categoria;
        this.marca = marca;
        this.modalidade = modalidade;
        this.especificacoesTecnicas = especificacoesTecnicas;
        this.tabelaGeometria = tabelaGeometria;
        this.imagens = imagens;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSlug() {
        return slug;
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

    public String getEspecificacoesTecnicas() {
        return especificacoesTecnicas;
    }

    public String getTabelaGeometria() {
        return tabelaGeometria;
    }

    public String getImagens() {
        return imagens;
    }

    public ProdutoStatus getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void atualizar(String nome, String descricao, String categoria, String marca, String modalidade,
                           String especificacoesTecnicas, String tabelaGeometria, String imagens) {
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.marca = marca;
        this.modalidade = modalidade;
        this.especificacoesTecnicas = especificacoesTecnicas;
        this.tabelaGeometria = tabelaGeometria;
        this.imagens = imagens;
        this.atualizadoEm = Instant.now();
    }

    public void atualizarStatus(ProdutoStatus status) {
        this.status = status;
        this.atualizadoEm = Instant.now();
    }
}
