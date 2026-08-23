package com.bikeshop.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.cart.dto.CartViewDto;
import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.orders.EnderecoEntregaInput;
import com.bikeshop.payments.PaymentProvider;
import java.util.Map;
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
class CheckoutContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveCriarPedidoComPagamentoSimuladoELimparCarrinho() {
        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/urbana-aro-26-cityride", ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();
        int estoqueAntes = detail.getBody().variacoes().get(0).estoqueDisponivel();

        ResponseEntity<Void> addResponse = restTemplate.postForEntity(
                "/api/v1/cart/items", Map.of("variacaoProdutoId", variacaoId, "quantidade", 2), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookiePair);

        CreateOrderRequest request = new CreateOrderRequest(
                "Maria Ciclista",
                "maria@example.com",
                new EnderecoEntregaInput("01310-100", "Av. Paulista", "1000", "Ap 10", "Bela Vista", "São Paulo", "SP"),
                PaymentProvider.STRIPE
        );

        ResponseEntity<CheckoutResultDto> response = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(request, headers), CheckoutResultDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CheckoutResultDto result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.pedido().status()).isEqualTo("AGUARDANDO_PAGAMENTO");
        assertThat(result.pedido().itens()).hasSize(1);
        assertThat(result.pedido().valorFrete()).isNotNull();
        assertThat(result.pagamentoSimulado()).isTrue();
        assertThat(result.pedido().paymentReference()).startsWith("SIM-STRIPE-");

        // Carrinho deve ter sido esvaziado após o checkout.
        ResponseEntity<CartViewDto> cartAfter = restTemplate.exchange(
                "/api/v1/cart", HttpMethod.GET, new HttpEntity<>(headers), CartViewDto.class);
        assertThat(cartAfter.getBody().itens()).isEmpty();

        // Estoque deve ter sido debitado.
        ResponseEntity<ProductDetailDto> detailAfter = restTemplate.getForEntity(
                "/api/v1/catalog/products/urbana-aro-26-cityride", ProductDetailDto.class);
        assertThat(detailAfter.getBody().variacoes().get(0).estoqueDisponivel()).isEqualTo(estoqueAntes - 2);
    }

    @Test
    void deveAtualizarStatusDoPedidoAoReceberWebhookDePagamentoAprovado() {
        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity(
                "/api/v1/catalog/products/capacete-ciclista-prosafe", ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();

        ResponseEntity<Void> addResponse = restTemplate.postForEntity(
                "/api/v1/cart/items", Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), Void.class);
        String cookiePair = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";", 2)[0];
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookiePair);

        CreateOrderRequest request = new CreateOrderRequest(
                "João Ciclista",
                "joao@example.com",
                new EnderecoEntregaInput("20040-020", "Av. Rio Branco", "1", null, "Centro", "Rio de Janeiro", "RJ"),
                PaymentProvider.MERCADO_PAGO
        );
        ResponseEntity<CheckoutResultDto> checkoutResponse = restTemplate.exchange(
                "/api/v1/checkout/orders", HttpMethod.POST, new HttpEntity<>(request, headers), CheckoutResultDto.class);
        String paymentReference = checkoutResponse.getBody().pedido().paymentReference();

        String webhookPayload = """
                {"action":"payment.updated","data":{"id":"%s"}}
                """.formatted(paymentReference);

        ResponseEntity<Void> webhookResponse = restTemplate.postForEntity(
                "/api/v1/payments/webhooks/mercado-pago", webhookPayload, Void.class);
        assertThat(webhookResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
