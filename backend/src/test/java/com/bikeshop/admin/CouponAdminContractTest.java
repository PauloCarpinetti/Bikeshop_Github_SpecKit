package com.bikeshop.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.admin.dto.CouponDto;
import com.bikeshop.admin.dto.CreateCouponRequest;
import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CouponQuoteResponseDto;
import com.bikeshop.checkout.dto.CouponRequest;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.LoginRequest;
import com.bikeshop.orders.EnderecoEntregaInput;
import com.bikeshop.payments.PaymentProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class CouponAdminContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveCriarValidarEAplicarCupomPercentualNoCheckout() {
        HttpHeaders adminHeaders = loginComoAdmin();
        String codigo = "PROMO" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        CreateCouponRequest createRequest = new CreateCouponRequest(
                codigo, "PERCENTUAL", new BigDecimal("10"),
                Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                null, null, null);
        ResponseEntity<CouponDto> createResponse = restTemplate.exchange(
                "/api/v1/admin/coupons", HttpMethod.POST, new HttpEntity<>(createRequest, adminHeaders), CouponDto.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().codigo()).isEqualTo(codigo);

        ResponseEntity<CouponDto[]> listResponse = restTemplate.exchange(
                "/api/v1/admin/coupons", HttpMethod.GET, new HttpEntity<>(adminHeaders), CouponDto[].class);
        assertThat(List.of(listResponse.getBody())).extracting(CouponDto::codigo).contains(codigo);

        // Monta carrinho e valida o cupom (preview) antes do checkout.
        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/speed-aro-700-veloce", ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();
        BigDecimal precoUnitario = detail.getBody().variacoes().get(0).preco();

        ResponseEntity<Void> addResponse = restTemplate.postForEntity(
                "/api/v1/cart/items", Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        HttpHeaders cartHeaders = new HttpHeaders();
        cartHeaders.add(HttpHeaders.COOKIE, cookiePair);

        ResponseEntity<CouponQuoteResponseDto> quoteResponse = restTemplate.exchange(
                "/api/v1/checkout/coupon", HttpMethod.POST, new HttpEntity<>(new CouponRequest(codigo), cartHeaders),
                CouponQuoteResponseDto.class);
        assertThat(quoteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        BigDecimal descontoEsperado = precoUnitario.multiply(new BigDecimal("0.10"));
        assertThat(quoteResponse.getBody().valorDesconto()).isEqualByComparingTo(descontoEsperado);

        // Aplica o cupom no pedido de fato.
        CreateOrderRequest orderRequest = new CreateOrderRequest(
                "Cliente Cupom", "cupom@example.com",
                new EnderecoEntregaInput("01310-100", "Av. Paulista", "1000", null, "Bela Vista", "São Paulo", "SP"),
                PaymentProvider.STRIPE, codigo);
        ResponseEntity<CheckoutResultDto> checkoutResponse = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(orderRequest, cartHeaders), CheckoutResultDto.class);
        assertThat(checkoutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkoutResponse.getBody().pedido().cupomCodigo()).isEqualTo(codigo);
        assertThat(checkoutResponse.getBody().pedido().valorDesconto()).isEqualByComparingTo(descontoEsperado);
        assertThat(checkoutResponse.getBody().pedido().valorTotal())
                .isEqualByComparingTo(checkoutResponse.getBody().pedido().valorItens()
                        .add(checkoutResponse.getBody().pedido().valorFrete())
                        .subtract(descontoEsperado));

        // Cupom de uso único (sem limite aqui, mas confirma que o contador de uso subiu).
        ResponseEntity<CouponDto[]> listAfter = restTemplate.exchange(
                "/api/v1/admin/coupons", HttpMethod.GET, new HttpEntity<>(adminHeaders), CouponDto[].class);
        CouponDto cupomAtualizado = List.of(listAfter.getBody()).stream()
                .filter(c -> c.codigo().equals(codigo)).findFirst().orElseThrow();
        assertThat(cupomAtualizado.usosRealizados()).isEqualTo(1);
    }

    @Test
    void deveRecusarCupomExpirado() {
        HttpHeaders adminHeaders = loginComoAdmin();
        String codigo = "EXPIRADO" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        CreateCouponRequest createRequest = new CreateCouponRequest(
                codigo, "VALOR_FIXO", new BigDecimal("50"),
                Instant.now().minus(10, ChronoUnit.DAYS), Instant.now().minus(1, ChronoUnit.DAYS),
                null, null, null);
        restTemplate.exchange("/api/v1/admin/coupons", HttpMethod.POST, new HttpEntity<>(createRequest, adminHeaders), CouponDto.class);

        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/capacete-ciclista-prosafe", ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();
        ResponseEntity<Void> addResponse = restTemplate.postForEntity(
                "/api/v1/cart/items", Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        HttpHeaders cartHeaders = new HttpHeaders();
        cartHeaders.add(HttpHeaders.COOKIE, cookiePair);

        ResponseEntity<Map> quoteResponse = restTemplate.exchange(
                "/api/v1/checkout/coupon", HttpMethod.POST, new HttpEntity<>(new CouponRequest(codigo), cartHeaders), Map.class);
        assertThat(quoteResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(quoteResponse.getBody().get("code")).isEqualTo("CUPOM_EXPIRADO");
    }

    private HttpHeaders loginComoAdmin() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new LoginRequest("admin@bikeshop.example", "admin12345"), AuthResponse.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(response.getBody().accessToken());
        return headers;
    }
}
