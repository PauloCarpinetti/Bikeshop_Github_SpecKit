package com.bikeshop.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.admin.dto.UpdateOrderStatusRequest;
import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.LoginRequest;
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
class OrderAdminContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveListarEAtualizarStatusDePedidoComoAdmin() {
        HttpHeaders adminHeaders = loginComoAdmin();

        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/urbana-aro-26-cityride", ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();

        ResponseEntity<Void> addResponse = restTemplate.postForEntity(
                "/api/v1/cart/items", Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        HttpHeaders cartHeaders = new HttpHeaders();
        cartHeaders.add(HttpHeaders.COOKIE, cookiePair);

        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "Cliente Admin Test", "adminorder@example.com",
                new EnderecoEntregaInput("01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
                PaymentProvider.STRIPE, null);
        ResponseEntity<CheckoutResultDto> checkoutResponse = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(orderRequest, cartHeaders), CheckoutResultDto.class);
        Long pedidoId = checkoutResponse.getBody().pedido().id();

        ResponseEntity<OrderDto[]> listResponse = restTemplate.exchange(
                "/api/v1/admin/orders", HttpMethod.GET, new HttpEntity<>(adminHeaders), OrderDto[].class);
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(listResponse.getBody())).extracting(OrderDto::id).contains(pedidoId);

        ResponseEntity<OrderDto> patchResponse = restTemplate.exchange(
                "/api/v1/admin/orders/" + pedidoId, HttpMethod.PATCH,
                new HttpEntity<>(new UpdateOrderStatusRequest("ENTREGUE"), adminHeaders), OrderDto.class);
        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody().status()).isEqualTo("ENTREGUE");
    }

    @Test
    void deveRecusarAcessoAdministrativoParaClienteComum() {
        String email = "cliente-comum-orders-" + UUID.randomUUID() + "@example.com";
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                "/api/v1/auth/register", new RegisterRequest("Cliente Comum", email, "senha12345"), AuthResponse.class);
        HttpHeaders customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(registerResponse.getBody().accessToken());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/orders", HttpMethod.GET, new HttpEntity<>(customerHeaders), Map.class);
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
