package com.bikeshop.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.RegisterRequest;
import com.bikeshop.orders.EnderecoEntregaInput;
import com.bikeshop.orders.OrderService;
import com.bikeshop.orders.PedidoStatus;
import com.bikeshop.orders.dto.OrderDto;
import com.bikeshop.orders.dto.ReturnRequest;
import com.bikeshop.payments.PaymentProvider;
import com.bikeshop.reviews.dto.CreateReviewRequest;
import com.bikeshop.reviews.dto.ReviewDto;
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
class PostSaleContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderService orderService;

    @Test
    void deveRecusarDevolucaoAntesDeEntregueEPermitirDepois() {
        HttpHeaders authHeaders = registrarCliente();
        Long pedidoId = criarPedido(authHeaders, "urbana-aro-26-cityride");

        ResponseEntity<Map> beforeResponse = restTemplate.exchange(
                "/api/v1/account/orders/" + pedidoId + "/return", HttpMethod.POST,
                new HttpEntity<>(new ReturnRequest("Produto com defeito"), authHeaders), Map.class);
        assertThat(beforeResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        orderService.atualizarStatus(pedidoId, PedidoStatus.ENTREGUE);

        ResponseEntity<OrderDto> afterResponse = restTemplate.exchange(
                "/api/v1/account/orders/" + pedidoId + "/return", HttpMethod.POST,
                new HttpEntity<>(new ReturnRequest("Produto com defeito"), authHeaders), OrderDto.class);
        assertThat(afterResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterResponse.getBody().status()).isEqualTo("EM_TROCA_DEVOLUCAO");
    }

    @Test
    void devePublicarAvaliacaoApenasParaPedidoEntregueERecusarDuplicata() {
        HttpHeaders authHeaders = registrarCliente();
        Long pedidoId = criarPedido(authHeaders, "speed-aro-700-veloce");
        ResponseEntity<OrderDto> orderDetail = restTemplate.exchange(
                "/api/v1/account/orders/" + pedidoId, HttpMethod.GET, new HttpEntity<>(authHeaders), OrderDto.class);
        Long variacaoId = orderDetail.getBody().itens().get(0).variacaoProdutoId();

        CreateReviewRequest reviewRequest = new CreateReviewRequest(pedidoId, variacaoId, 5, "Ótima bicicleta!");
        ResponseEntity<Map> beforeResponse = restTemplate.exchange(
                "/api/v1/account/reviews", HttpMethod.POST, new HttpEntity<>(reviewRequest, authHeaders), Map.class);
        assertThat(beforeResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        orderService.atualizarStatus(pedidoId, PedidoStatus.ENTREGUE);

        ResponseEntity<ReviewDto> createdResponse = restTemplate.exchange(
                "/api/v1/account/reviews", HttpMethod.POST, new HttpEntity<>(reviewRequest, authHeaders), ReviewDto.class);
        assertThat(createdResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createdResponse.getBody().status()).isEqualTo("PUBLICADA");

        ResponseEntity<Map> duplicateResponse = restTemplate.exchange(
                "/api/v1/account/reviews", HttpMethod.POST, new HttpEntity<>(reviewRequest, authHeaders), Map.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private HttpHeaders registrarCliente() {
        String email = "postsale-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Cliente PosVenda", email, "senha12345");
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
                "Cliente PosVenda", "postsale@example.com",
                new EnderecoEntregaInput("01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
                PaymentProvider.STRIPE, null
        );
        ResponseEntity<CheckoutResultDto> checkoutResponse = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(orderRequest, checkoutHeaders), CheckoutResultDto.class);
        return checkoutResponse.getBody().pedido().id();
    }
}
