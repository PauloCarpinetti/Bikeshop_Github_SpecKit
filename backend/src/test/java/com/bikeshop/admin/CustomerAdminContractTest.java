package com.bikeshop.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.admin.dto.CustomerDto;
import com.bikeshop.admin.dto.UpdateCustomerStatusRequest;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.LoginRequest;
import com.bikeshop.customers.dto.RegisterRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CustomerAdminContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveListarClienteEBloquearImpedindoLoginPosterior() {
        HttpHeaders adminHeaders = loginComoAdmin();
        String email = "cliente-admin-" + UUID.randomUUID() + "@example.com";
        String senha = "senha12345";
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest("Cliente Backoffice", email, senha), AuthResponse.class);
        Long clienteId = registerResponse.getBody().clienteId();

        ResponseEntity<CustomerDto[]> listResponse = restTemplate.exchange(
                "/api/v1/admin/customers", HttpMethod.GET, new HttpEntity<>(adminHeaders), CustomerDto[].class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody()))
                .anySatisfy(c -> {
                    assertThat(c.id()).isEqualTo(clienteId);
                    assertThat(c.bloqueado()).isFalse();
                });

        ResponseEntity<CustomerDto> blockResponse = restTemplate.exchange(
                "/api/v1/admin/customers/" + clienteId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(new UpdateCustomerStatusRequest(true), adminHeaders), CustomerDto.class);
        assertThat(blockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(blockResponse.getBody().bloqueado()).isTrue();

        ResponseEntity<Map> loginAposBloqueio = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, senha), Map.class);
        assertThat(loginAposBloqueio.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(loginAposBloqueio.getBody().get("code")).isEqualTo("CONTA_BLOQUEADA");

        ResponseEntity<CustomerDto> unblockResponse = restTemplate.exchange(
                "/api/v1/admin/customers/" + clienteId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(new UpdateCustomerStatusRequest(false), adminHeaders), CustomerDto.class);
        assertThat(unblockResponse.getBody().bloqueado()).isFalse();

        ResponseEntity<AuthResponse> loginAposDesbloqueio = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, senha), AuthResponse.class);
        assertThat(loginAposDesbloqueio.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deveRecusarAcessoAdministrativoParaClienteComum() {
        String email = "cliente-comum-customers-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest("Cliente Comum", email, "senha12345"), AuthResponse.class);
        HttpHeaders customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(registerResponse.getBody().accessToken());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/customers", HttpMethod.GET, new HttpEntity<>(customerHeaders), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpHeaders loginComoAdmin() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest("admin@bikeshop.example", "admin12345"), AuthResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
    }
}
