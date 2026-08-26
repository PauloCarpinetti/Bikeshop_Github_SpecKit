package com.bikeshop.customers;

import com.bikeshop.common.security.Role;
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
 * Cliente (spec.md, Key Entities): usuário autenticado da loja (FR-008).
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CUSTOMER;

    @Column(nullable = false)
    private boolean bloqueado = false;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    protected Cliente() {
        // JPA
    }

    public Cliente(String nome, String email, String senhaHash) {
        this(nome, email, senhaHash, Role.CUSTOMER);
    }

    public Cliente(String nome, String email, String senhaHash, Role role) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getTelefone() {
        return telefone;
    }

    public Role getRole() {
        return role;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    void updateNome(String nome) {
        this.nome = nome;
    }

    void updateTelefone(String telefone) {
        this.telefone = telefone;
    }

    void updateSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    // Público: gerido pelo backoffice (com.bikeshop.admin), pacote diferente do cliente (FR-009).
    public void bloquear() {
        this.bloqueado = true;
    }

    public void desbloquear() {
        this.bloqueado = false;
    }
}
