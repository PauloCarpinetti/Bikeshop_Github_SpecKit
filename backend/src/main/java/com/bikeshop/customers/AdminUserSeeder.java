package com.bikeshop.customers;

import com.bikeshop.common.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria um usuário administrativo de desenvolvimento na primeira subida (Fase 5), já que o cadastro
 * público (`/auth/register`) só cria clientes com papel CUSTOMER. Não roda se o e-mail já existir
 * (mesmo padrão idempotente do {@link com.bikeshop.catalog.CatalogDataSeeder}).
 */
@Component
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminUserSeeder(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder,
                            @Value("${bikeshop.admin.seed-email:admin@bikeshop.example}") String adminEmail,
                            @Value("${bikeshop.admin.seed-password:admin12345}") String adminPassword) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (clienteRepository.existsByEmail(adminEmail)) {
            log.info("Usuário administrativo já existe, seed ignorado.");
            return;
        }

        Cliente admin = new Cliente("Administrador BikeShop", adminEmail, passwordEncoder.encode(adminPassword), Role.ADMIN);
        clienteRepository.save(admin);
        log.info("Usuário administrativo de desenvolvimento criado: {} (login via /auth/login)", adminEmail);
    }
}
