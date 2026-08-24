package com.bikeshop.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Cupom de Desconto (spec.md, Key Entities; FR-009): criado no backoffice, consumido no checkout
 * (ver {@link com.bikeshop.checkout.CouponService}).
 */
@Entity
@Table(name = "cupom_desconto")
public class CupomDesconto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CupomTipo tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "valido_de", nullable = false)
    private Instant validoDe;

    @Column(name = "valido_ate", nullable = false)
    private Instant validoAte;

    @Column(name = "valor_minimo_carrinho", precision = 10, scale = 2)
    private BigDecimal valorMinimoCarrinho;

    /** Categorias às quais o cupom se aplica; nulo/vazio = qualquer categoria. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categorias_aplicaveis")
    private String categoriasAplicaveis;

    @Column(name = "limite_de_uso")
    private Integer limiteDeUso;

    @Column(name = "usos_realizados", nullable = false)
    private int usosRealizados;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected CupomDesconto() {
        // JPA
    }

    public CupomDesconto(String codigo, CupomTipo tipo, BigDecimal valor, Instant validoDe, Instant validoAte,
                          BigDecimal valorMinimoCarrinho, String categoriasAplicaveis, Integer limiteDeUso) {
        this.codigo = codigo;
        this.tipo = tipo;
        this.valor = valor;
        this.validoDe = validoDe;
        this.validoAte = validoAte;
        this.valorMinimoCarrinho = valorMinimoCarrinho;
        this.categoriasAplicaveis = categoriasAplicaveis;
        this.limiteDeUso = limiteDeUso;
        this.usosRealizados = 0;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public CupomTipo getTipo() {
        return tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Instant getValidoDe() {
        return validoDe;
    }

    public Instant getValidoAte() {
        return validoAte;
    }

    public BigDecimal getValorMinimoCarrinho() {
        return valorMinimoCarrinho;
    }

    public String getCategoriasAplicaveis() {
        return categoriasAplicaveis;
    }

    public Integer getLimiteDeUso() {
        return limiteDeUso;
    }

    public int getUsosRealizados() {
        return usosRealizados;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void atualizar(CupomTipo tipo, BigDecimal valor, Instant validoDe, Instant validoAte,
                           BigDecimal valorMinimoCarrinho, String categoriasAplicaveis, Integer limiteDeUso) {
        this.tipo = tipo;
        this.valor = valor;
        this.validoDe = validoDe;
        this.validoAte = validoAte;
        this.valorMinimoCarrinho = valorMinimoCarrinho;
        this.categoriasAplicaveis = categoriasAplicaveis;
        this.limiteDeUso = limiteDeUso;
    }

    /** Desativação (T079: DELETE) — expira o cupom imediatamente em vez de apagá-lo, preservando
     * o histórico de pedidos que já o referenciam. */
    public void expirarAgora() {
        this.validoAte = Instant.now();
    }

    public void registrarUso() {
        this.usosRealizados++;
    }
}
