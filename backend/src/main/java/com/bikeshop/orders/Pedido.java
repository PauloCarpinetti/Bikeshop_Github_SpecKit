package com.bikeshop.orders;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Pedido (spec.md, Key Entities): itens, valores, frete, status e histórico de mudanças (FR-007).
 */
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id")
    private String cartId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "cliente_nome", nullable = false)
    private String clienteNome;

    @Column(name = "cliente_email", nullable = false)
    private String clienteEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "endereco_entrega", nullable = false)
    private String enderecoEntrega;

    @Column(name = "valor_itens", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorItens;

    @Column(name = "valor_frete", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorFrete;

    @Column(name = "transportadora")
    private String transportadora;

    @Column(name = "prazo_frete_dias")
    private Integer prazoFreteDias;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "cupom_codigo")
    private String cupomCodigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PedidoStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "status_historico", nullable = false)
    private String statusHistorico;

    @Column(name = "payment_provider")
    private String paymentProvider;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
        // JPA
    }

    public Pedido(String cartId, Long clienteId, String clienteNome, String clienteEmail, String enderecoEntrega,
                  BigDecimal valorItens, BigDecimal valorFrete, String transportadora, Integer prazoFreteDias,
                  String statusHistoricoJson) {
        this.cartId = cartId;
        this.clienteId = clienteId;
        this.clienteNome = clienteNome;
        this.clienteEmail = clienteEmail;
        this.enderecoEntrega = enderecoEntrega;
        this.valorItens = valorItens;
        this.valorFrete = valorFrete;
        this.transportadora = transportadora;
        this.prazoFreteDias = prazoFreteDias;
        this.valorTotal = valorItens.add(valorFrete);
        this.status = PedidoStatus.CRIADO;
        this.statusHistorico = statusHistoricoJson;
    }

    public Long getId() {
        return id;
    }

    public String getCartId() {
        return cartId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteEmail() {
        return clienteEmail;
    }

    public String getEnderecoEntrega() {
        return enderecoEntrega;
    }

    public BigDecimal getValorItens() {
        return valorItens;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public String getTransportadora() {
        return transportadora;
    }

    public Integer getPrazoFreteDias() {
        return prazoFreteDias;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getCupomCodigo() {
        return cupomCodigo;
    }

    public PedidoStatus getStatus() {
        return status;
    }

    void setStatus(PedidoStatus status) {
        this.status = status;
        this.atualizadoEm = Instant.now();
    }

    public String getStatusHistorico() {
        return statusHistorico;
    }

    void setStatusHistorico(String statusHistorico) {
        this.statusHistorico = statusHistorico;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    void setPayment(String provider, String reference, String status) {
        this.paymentProvider = provider;
        this.paymentReference = reference;
        this.paymentStatus = status;
        this.atualizadoEm = Instant.now();
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public List<ItemPedido> getItens() {
        return itens;
    }

    public void addItem(ItemPedido item) {
        itens.add(item);
    }
}
