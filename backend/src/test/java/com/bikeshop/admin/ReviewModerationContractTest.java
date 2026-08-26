package com.bikeshop.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.LoginRequest;
import com.bikeshop.customers.dto.RegisterRequest;
import com.bikeshop.orders.EnderecoEntregaInput;
import com.bikeshop.orders.OrderService;
import com.bikeshop.orders.PedidoStatus;
import com.bikeshop.orders.dto.OrderDto;
import com.bikeshop.payments.PaymentProvider;
import com.bikeshop.reviews.dto.CreateReviewRequest;
import com.bikeshop.reviews.dto.ModerateReviewRequest;
import com.bikeshop.reviews.dto.ReviewAdminDto;
import com.bikeshop.reviews.dto.ReviewDto;
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
class ReviewModerationContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderService orderService;

    @Test
    void deveListarEModerarAvaliacaoRejeitandoEAprovandoDeVolta() {
        HttpHeaders authHeaders = registrarCliente();
        Long pedidoId = criarPedido(authHeaders, "urbana-aro-26-cityride");
        orderService.atualizarStatus(pedidoId, PedidoStatus.ENTREGUE);

        ResponseEntity<OrderDto> orderDetail = restTemplate.exchange(
                "/api/v1/account/orders/" + pedidoId, HttpMethod.GET, new HttpEntity<>(authHeaders), OrderDto.class);
        Long variacaoId = orderDetail.getBody().itens().get(0).variacaoProdutoId();

        ResponseEntity<ReviewDto> reviewResponse = restTemplate.exchange(
                "/api/v1/account/reviews", HttpMethod.POST,
                new HttpEntity<>(new CreateReviewRequest(pedidoId, variacaoId, 4, "Muito boa"), authHeaders), ReviewDto.class);
        Long reviewId = reviewResponse.getBody().id();

        HttpHeaders adminHeaders = loginComoAdmin();
        ResponseEntity<ReviewAdminDto[]> listResponse = restTemplate.exchange(
                "/api/v1/admin/reviews", HttpMethod.GET, new HttpEntity<>(adminHeaders), ReviewAdminDto[].class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody()))
                .anySatisfy(r -> {
                    assertThat(r.id()).isEqualTo(reviewId);
                    assertThat(r.status()).isEqualTo("PUBLICADA");
                });

        ResponseEntity<ReviewAdminDto> rejectResponse = restTemplate.exchange(
                "/api/v1/admin/reviews/" + reviewId, HttpMethod.PATCH,
                new HttpEntity<>(new ModerateReviewRequest(false), adminHeaders), ReviewAdminDto.class);
        assertThat(rejectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rejectResponse.getBody().status()).isEqualTo("MODERADA");

        ResponseEntity<ReviewAdminDto> approveResponse = restTemplate.exchange(
                "/api/v1/admin/reviews/" + reviewId, HttpMethod.PATCH,
                new HttpEntity<>(new ModerateReviewRequest(true), adminHeaders), ReviewAdminDto.class);
        assertThat(approveResponse.getBody().status()).isEqualTo("PUBLICADA");
    }

    @Test
    void deveRecusarAcessoAdministrativoParaClienteComum() {
        String email = "cliente-comum-reviews-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest("Cliente Comum", email, "senha12345"), AuthResponse.class);
        HttpHeaders customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(registerResponse.getBody().accessToken());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/reviews", HttpMethod.GET, new HttpEntity<>(customerHeaders), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private HttpHeaders registrarCliente() {
        String email = "review-mod-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Cliente Moderacao", email, "senha12345");
        ResponseEntity<AuthResponse> response =
                restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
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
                "Cliente Moderacao", "review-mod@example.com",
                new EnderecoEntregaInput("01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
                PaymentProvider.STRIPE, null);
        ResponseEntity<CheckoutResultDto> checkoutResponse = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(orderRequest, checkoutHeaders), CheckoutResultDto.class);
        return checkoutResponse.getBody().pedido().id();
    }

    private HttpHeaders loginComoAdmin() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest("admin@bikeshop.example", "admin12345"), AuthResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
    }
}
