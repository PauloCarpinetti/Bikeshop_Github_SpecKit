package com.bikeshop.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.cart.dto.CartViewDto;
import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.LoginRequest;
import com.bikeshop.customers.dto.RefreshRequest;
import com.bikeshop.customers.dto.RegisterRequest;
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
class AuthContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveCadastrarLogarERecusarEmailDuplicado() {
        String email = "cliente-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Cliente Teste", email, "senha12345");

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().accessToken()).isNotBlank();
        assertThat(registerResponse.getBody().email()).isEqualTo(email);

        ResponseEntity<Map> duplicateResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, Map.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "senha12345"), AuthResponse.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody().clienteId()).isEqualTo(registerResponse.getBody().clienteId());

        ResponseEntity<Map> wrongPasswordResponse = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest(email, "senhaErrada"), Map.class);
        assertThat(wrongPasswordResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ResponseEntity<AuthResponse> refreshResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(loginResponse.getBody().refreshToken()), AuthResponse.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody().accessToken()).isNotBlank();
    }

    @Test
    void deveMesclarCarrinhoDeVisitanteAoCadastrar() {
        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/speed-aro-700-veloce", ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();

        ResponseEntity<Void> addResponse = restTemplate.postForEntity(
                "/api/v1/cart/items", Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookiePair);

        String email = "carrinho-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Cliente Carrinho", email, "senha12345");
        restTemplate.exchange("/api/v1/auth/register", HttpMethod.POST, new HttpEntity<>(registerRequest, headers), AuthResponse.class);

        ResponseEntity<CartViewDto> cartAfterRegister = restTemplate.exchange(
                "/api/v1/cart", HttpMethod.GET, new HttpEntity<>(headers), CartViewDto.class);
        assertThat(cartAfterRegister.getBody().itens()).hasSize(1);
        assertThat(cartAfterRegister.getBody().itens().get(0).variacaoProdutoId()).isEqualTo(variacaoId);
    }
}
