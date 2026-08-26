package com.bikeshop.admin.dto;

import java.time.Instant;

public record CustomerDto(Long id, String nome, String email, String telefone, boolean bloqueado, Instant criadoEm) {
}
