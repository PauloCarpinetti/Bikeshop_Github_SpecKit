package com.bikeshop.customers;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.RegisterRequest;
import com.bikeshop.orders.EnderecoEntregaInput;
import com.bikeshop.orders.dto.OrderDto;
import com.bikeshop.payments.PaymentProvider;
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
class AccountOrdersContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveListarEDetalharPedidosDoClienteAutenticado() {
        String email = "pedidos-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Cliente Pedidos", email, "senha12345");
        ResponseEntity<AuthResponse> authResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);
        String accessToken = authResponse.getBody().accessToken();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);

        // Nenhum pedido ainda.
        ResponseEntity<OrderDto[]> emptyList = restTemplate.exchange(
                "/api/v1/account/orders", HttpMethod.GET, new HttpEntity<>(authHeaders), OrderDto[].class);
        assertThat(emptyList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(emptyList.getBody()).isEmpty();

        // Monta carrinho (visitante) e finaliza o checkout autenticado (Authorization + cookie).
        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/mountain-bike-aro-29-explorer", ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();

        ResponseEntity<Void> addResponse = restTemplate.exchange(
                "/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), authHeaders), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];

        HttpHeaders checkoutHeaders = new HttpHeaders();
        checkoutHeaders.setBearerAuth(accessToken);
        checkoutHeaders.add(HttpHeaders.COOKIE, cookiePair);

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "Cliente Pedidos", email,
                new EnderecoEntregaInput("01310-100", "Av. Paulista", "1000", "Ap 10", "Bela Vista", "São Paulo", "SP"),
                PaymentProvider.STRIPE
        );
        ResponseEntity<CheckoutResultDto> checkoutResponse = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(orderRequest, checkoutHeaders), CheckoutResultDto.class);
        Long pedidoId = checkoutResponse.getBody().pedido().id();

        // Agora o histórico deve conter o pedido.
        ResponseEntity<OrderDto[]> listResponse = restTemplate.exchange(
                "/api/v1/account/orders", HttpMethod.GET, new HttpEntity<>(authHeaders), OrderDto[].class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody())).extracting(OrderDto::id).containsExactly(pedidoId);

        ResponseEntity<OrderDto> detailResponse = restTemplate.exchange(
                "/api/v1/account/orders/" + pedidoId, HttpMethod.GET, new HttpEntity<>(authHeaders), OrderDto.class);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody().itens()).hasSize(1);
        assertThat(detailResponse.getBody().statusHistorico()).isNotEmpty();
        assertThat(detailResponse.getBody().enderecoEntrega().cidade()).isEqualTo("São Paulo");
    }

    @Test
    void deveNegarAcessoAPedidoDeOutroCliente() {
        HttpHeaders headersDono = registrarERetornarHeaders();
        HttpHeaders headersOutro = registrarERetornarHeaders();

        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/speed-aro-700-veloce", ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();

        ResponseEntity<Void> addResponse = restTemplate.exchange(
                "/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), headersDono), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        headersDono.add(HttpHeaders.COOKIE, cookiePair);

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "Dono do Pedido", "dono@example.com",
                new EnderecoEntregaInput("20040-020", "Av. Rio Branco", "1", null, "Centro", "Rio de Janeiro", "RJ"),
                PaymentProvider.PAGSEGURO
        );
        ResponseEntity<CheckoutResultDto> checkoutResponse = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(orderRequest, headersDono), CheckoutResultDto.class);
        Long pedidoId = checkoutResponse.getBody().pedido().id();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/account/orders/" + pedidoId, HttpMethod.GET, new HttpEntity<>(headersOutro), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private HttpHeaders registrarERetornarHeaders() {
        String email = "cliente-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Cliente " + UUID.randomUUID(), email, "senha12345");
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
    }
}
