package com.bikeshop.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.admin.dto.AuditLogDto;
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
class AuditLogContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveRegistrarEConsultarAcaoAdministrativaSensivelNoLogDeAuditoria() {
        HttpHeaders adminHeaders = loginComoAdmin();
        String email = "cliente-audit-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest("Cliente Auditoria", email, "senha12345"), AuthResponse.class);
        Long clienteId = registerResponse.getBody().clienteId();

        restTemplate.exchange(
                "/api/v1/admin/customers/" + clienteId + "/status", HttpMethod.PATCH,
                new HttpEntity<>(new UpdateCustomerStatusRequest(true), adminHeaders), Map.class);

        ResponseEntity<AuditLogDto[]> logsResponse = restTemplate.exchange(
                "/api/v1/admin/audit-logs", HttpMethod.GET, new HttpEntity<>(adminHeaders), AuditLogDto[].class);
        assertThat(logsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(logsResponse.getBody()))
                .anySatisfy(l -> {
                    assertThat(l.action()).isEqualTo("ATUALIZAR_STATUS_CLIENTE");
                    assertThat(l.entityName()).isEqualTo("Cliente");
                    assertThat(l.entityId()).isEqualTo(String.valueOf(clienteId));
                });
    }

    @Test
    void deveRecusarAcessoAdministrativoParaClienteComum() {
        String email = "cliente-comum-audit-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest("Cliente Comum", email, "senha12345"), AuthResponse.class);
        HttpHeaders customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(registerResponse.getBody().accessToken());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/audit-logs", HttpMethod.GET, new HttpEntity<>(customerHeaders), Map.class);
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
