package com.bikeshop.reviews;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Avaliação de produto (data-model.md): só pode ser criada para um produto de um pedido do
 * próprio cliente confirmado como entregue (US2, regra aplicada em {@link ReviewService}).
 */
@Entity
@Table(name = "avaliacao")
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "produto_id", nullable = false)
    private Long produtoId;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private int nota;

    @Column
    private String comentario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AvaliacaoStatus status = AvaliacaoStatus.PUBLICADA;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Avaliacao() {
        // JPA
    }

    public Avaliacao(Long produtoId, Long clienteId, Long pedidoId, int nota, String comentario) {
        this.produtoId = produtoId;
        this.clienteId = clienteId;
        this.pedidoId = pedidoId;
        this.nota = nota;
        this.comentario = comentario;
        this.status = AvaliacaoStatus.PUBLICADA;
    }

    public Long getId() {
        return id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public int getNota() {
        return nota;
    }

    public String getComentario() {
        return comentario;
    }

    public AvaliacaoStatus getStatus() {
        return status;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    void moderar(AvaliacaoStatus status) {
        this.status = status;
    }
}
