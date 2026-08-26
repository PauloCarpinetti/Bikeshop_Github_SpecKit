package com.bikeshop.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.LoginRequest;
import com.bikeshop.customers.dto.LogoutRequest;
import com.bikeshop.customers.dto.ProfileDto;
import com.bikeshop.customers.dto.RefreshRequest;
import com.bikeshop.customers.dto.RegisterRequest;
import com.bikeshop.orders.EnderecoEntregaInput;
import com.bikeshop.payments.CreatePaymentIntentRequest;
import com.bikeshop.payments.PaymentProvider;
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

/**
 * Hardening de segurança da Fase 6 (T093): logout revoga token via blacklist Redis, refresh
 * revogado não emite novos tokens, e criação de intenção de pagamento respeita ownership do
 * pedido.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentSecurityContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void logoutDeveRevogarAccessTokenImediatamente() {
        AuthResponse auth = registrar("logout-access");
        HttpHeaders headers = bearer(auth.accessToken());

        ResponseEntity<ProfileDto> beforeLogout = restTemplate.exchange(
                "/api/v1/account/profile", HttpMethod.GET, new HttpEntity<>(headers), ProfileDto.class);
        assertThat(beforeLogout.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> logoutResponse = restTemplate.postForEntity(
                "/api/v1/auth/logout", new HttpEntity<>(new LogoutRequest(null), headers), Void.class);
        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> afterLogout = restTemplate.exchange(
                "/api/v1/account/profile", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(afterLogout.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void logoutDeveRevogarRefreshTokenImpedindoNovoRefresh() {
        AuthResponse auth = registrar("logout-refresh");

        restTemplate.postForEntity("/api/v1/auth/logout",
                new HttpEntity<>(new LogoutRequest(auth.refreshToken()), bearer(auth.accessToken())), Void.class);

        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshRequest(auth.refreshToken()), Map.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refreshResponse.getBody().get("code")).isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void deveRecusarCriacaoDeIntencaoDePagamentoParaPedidoDeOutroCliente() {
        AuthResponse donoDoPedido = registrar("dono-pedido");
        Long pedidoId = criarPedido(bearer(donoDoPedido.accessToken()), "urbana-aro-26-cityride");

        AuthResponse outroCliente = registrar("outro-cliente");
        ResponseEntity<Map> intentResponse = restTemplate.postForEntity(
                "/api/v1/payments/" + pedidoId + "/intents",
                new HttpEntity<>(new CreatePaymentIntentRequest(PaymentProvider.STRIPE), bearer(outroCliente.accessToken())),
                Map.class);
        assertThat(intentResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private AuthResponse registrar(String prefixo) {
        String email = prefixo + "-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest("Cliente " + prefixo, email, "senha12345"), AuthResponse.class);
        return response.getBody();
    }

    private Long criarPedido(HttpHeaders authHeaders, String slug) {
        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/" + slug, ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();

        ResponseEntity<Void> addResponse = restTemplate.exchange(
                "/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), authHeaders), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];

        HttpHeaders checkoutHeaders = new HttpHeaders();
        checkoutHeaders.putAll(authHeaders);
        checkoutHeaders.add(HttpHeaders.COOKIE, cookiePair);

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "Cliente Ownership", "ownership@example.com",
                new EnderecoEntregaInput("01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
                PaymentProvider.STRIPE, null);
        ResponseEntity<CheckoutResultDto> checkoutResponse = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(orderRequest, checkoutHeaders), CheckoutResultDto.class);
        return checkoutResponse.getBody().pedido().id();
    }

    private HttpHeaders bearer(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return headers;
    }
}
