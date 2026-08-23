package com.bikeshop.checkout;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.checkout.dto.ShippingQuoteRequest;
import com.bikeshop.checkout.dto.ShippingQuoteResponseDto;
import java.math.BigDecimal;
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
class ShippingQuoteContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveCalcularFreteEstimadoParaCarrinhoComItens() {
        HttpHeaders headers = criarCarrinhoComItem("mountain-bike-aro-29-explorer");

        ResponseEntity<ShippingQuoteResponseDto> response = restTemplate.exchange(
                "/api/v1/checkout/shipping-quote", HttpMethod.POST,
                new HttpEntity<>(new ShippingQuoteRequest("01310-100"), headers), ShippingQuoteResponseDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().transportadora()).isEqualTo("Correios");
        assertThat(response.getBody().valor()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.getBody().prazoDias()).isGreaterThan(0);
        // Sem credenciais reais dos Correios configuradas neste ambiente -> estimativa local.
        assertThat(response.getBody().estimado()).isTrue();
    }

    @Test
    void deveRejeitarCepInvalido() {
        HttpHeaders headers = criarCarrinhoComItem("capacete-ciclista-prosafe");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/checkout/shipping-quote", HttpMethod.POST,
                new HttpEntity<>(new ShippingQuoteRequest("abc"), headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deveRejeitarCotacaoSemCarrinho() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/checkout/shipping-quote", new ShippingQuoteRequest("01310-100"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private HttpHeaders criarCarrinhoComItem(String slug) {
        ResponseEntity<ProductDetailDto> detail = restTemplate.getForEntity("/api/v1/catalog/products/" + slug, ProductDetailDto.class);
        Long variacaoId = detail.getBody().variacoes().get(0).id();

        ResponseEntity<Void> addResponse = restTemplate.postForEntity(
                "/api/v1/cart/items", Map.of("variacaoProdutoId", variacaoId, "quantidade", 1), Void.class);
        String setCookie = addResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);
        return headers;
    }
}
