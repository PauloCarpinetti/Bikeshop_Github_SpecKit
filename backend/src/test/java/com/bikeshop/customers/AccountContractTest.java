package com.bikeshop.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.EnderecoDto;
import com.bikeshop.customers.dto.EnderecoRequest;
import com.bikeshop.customers.dto.ProfileDto;
import com.bikeshop.customers.dto.RegisterRequest;
import com.bikeshop.customers.dto.UpdateProfileRequest;
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
class AccountContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveConsultarEAtualizarPerfil() {
        HttpHeaders headers = registrarClienteEAutenticar("perfil");

        ResponseEntity<ProfileDto> getResponse = restTemplate.exchange(
                "/api/v1/account/profile", HttpMethod.GET, new HttpEntity<>(headers), ProfileDto.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().nome()).isEqualTo("Cliente Perfil");

        UpdateProfileRequest updateRequest = new UpdateProfileRequest("Cliente Atualizado", "11999999999", null);
        ResponseEntity<ProfileDto> putResponse = restTemplate.exchange(
                "/api/v1/account/profile", HttpMethod.PUT, new HttpEntity<>(updateRequest, headers), ProfileDto.class);
        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(putResponse.getBody().nome()).isEqualTo("Cliente Atualizado");
        assertThat(putResponse.getBody().telefone()).isEqualTo("11999999999");
    }

    @Test
    void deveRecusarAcessoAoPerfilSemToken() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/account/profile", Map.class);
        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void deveCriarListarEAtualizarEnderecos() {
        HttpHeaders headers = registrarClienteEAutenticar("enderecos");

        EnderecoRequest createRequest = new EnderecoRequest(
                "01310-100", "Av. Paulista", "1000", "Ap 10", "Bela Vista", "São Paulo", "SP", "ENTREGA", true);
        ResponseEntity<EnderecoDto> createResponse = restTemplate.exchange(
                "/api/v1/account/addresses", HttpMethod.POST, new HttpEntity<>(createRequest, headers), EnderecoDto.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().padrao()).isTrue();
        Long enderecoId = createResponse.getBody().id();

        ResponseEntity<EnderecoDto[]> listResponse = restTemplate.exchange(
                "/api/v1/account/addresses", HttpMethod.GET, new HttpEntity<>(headers), EnderecoDto[].class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody())).hasSize(1);

        EnderecoRequest updateRequest = new EnderecoRequest(
                "01310-100", "Av. Paulista", "1500", null, "Bela Vista", "São Paulo", "SP", "ENTREGA", true);
        ResponseEntity<EnderecoDto> updateResponse = restTemplate.exchange(
                "/api/v1/account/addresses/" + enderecoId, HttpMethod.PUT,
                new HttpEntity<>(updateRequest, headers), EnderecoDto.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().numero()).isEqualTo("1500");
    }

    private HttpHeaders registrarClienteEAutenticar(String prefixo) {
        String email = prefixo + "-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Cliente " + capitalize(prefixo), email, "senha12345");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
