package com.bikeshop.customers;

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
 * Endereço salvo na conta do cliente (data-model.md, subentidade de Cliente). Um pedido guarda um
 * snapshot próprio do endereço de entrega (ver {@code Pedido.enderecoEntrega}), não uma referência
 * viva a este registro.
 */
@Entity
@Table(name = "endereco")
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private String cep;

    @Column(nullable = false)
    private String logradouro;

    @Column(nullable = false)
    private String numero;

    @Column
    private String complemento;

    @Column(nullable = false)
    private String bairro;

    @Column(nullable = false)
    private String cidade;

    @Column(nullable = false)
    private String estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnderecoTipo tipo;

    @Column(nullable = false)
    private boolean padrao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Endereco() {
        // JPA
    }

    public Endereco(Long clienteId, String cep, String logradouro, String numero, String complemento,
                     String bairro, String cidade, String estado, EnderecoTipo tipo, boolean padrao) {
        this.clienteId = clienteId;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.tipo = tipo;
        this.padrao = padrao;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }

    public EnderecoTipo getTipo() {
        return tipo;
    }

    public boolean isPadrao() {
        return padrao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    void marcarComoPadrao(boolean padrao) {
        this.padrao = padrao;
    }

    void atualizar(String cep, String logradouro, String numero, String complemento, String bairro,
                    String cidade, String estado, EnderecoTipo tipo, boolean padrao) {
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.complemento = complemento;
        this.bairro = bairro;
        this.cidade = cidade;
        this.estado = estado;
        this.tipo = tipo;
        this.padrao = padrao;
    }
}
