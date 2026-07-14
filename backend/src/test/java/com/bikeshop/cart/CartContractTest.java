package com.bikeshop.cart;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.cart.dto.AddCartItemRequest;
import com.bikeshop.cart.dto.CartViewDto;
import com.bikeshop.cart.dto.UpdateCartItemRequest;
import com.bikeshop.catalog.dto.ProductDetailDto;
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
class CartContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveCriarCarrinhoEAdicionarAtualizarRemoverItem() {
        ResponseEntity<CartViewDto> getResponse = restTemplate.getForEntity("/api/v1/cart", CartViewDto.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().itens()).isEmpty();

        String setCookie = getResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("bikeshop_cart_id");
        // O header Set-Cookie traz atributos (Path, Max-Age, Expires...) que não são válidos no
        // header Cookie de requisição — extraímos apenas o par nome=valor.
        String cookiePair = setCookie.split(";", 2)[0];

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, cookiePair);

        Long variacaoId = variacaoIdDoSeed("mountain-bike-aro-29-explorer");

        ResponseEntity<CartViewDto> addResponse = restTemplate.exchange(
                "/api/v1/cart/items", HttpMethod.POST,
                new HttpEntity<>(new AddCartItemRequest(variacaoId, 2), headers), CartViewDto.class);
        assertThat(addResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addResponse.getBody().itens()).hasSize(1);
        assertThat(addResponse.getBody().itens().get(0).quantidade()).isEqualTo(2);

        ResponseEntity<CartViewDto> updateResponse = restTemplate.exchange(
                "/api/v1/cart/items/" + variacaoId, HttpMethod.PATCH,
                new HttpEntity<>(new UpdateCartItemRequest(5), headers), CartViewDto.class);
        assertThat(updateResponse.getBody().itens().get(0).quantidade()).isEqualTo(5);

        ResponseEntity<CartViewDto> deleteResponse = restTemplate.exchange(
                "/api/v1/cart/items/" + variacaoId, HttpMethod.DELETE,
                new HttpEntity<>(headers), CartViewDto.class);
        assertThat(deleteResponse.getBody().itens()).isEmpty();
    }

    @Test
    void deveRejeitarQuantidadeAcimaDoEstoqueDisponivel() {
        Long variacaoId = variacaoIdDoSeed("capacete-ciclista-prosafe");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/cart/items", new AddCartItemRequest(variacaoId, 999_999), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private Long variacaoIdDoSeed(String slug) {
        ResponseEntity<ProductDetailDto> response = restTemplate.getForEntity("/api/v1/catalog/products/" + slug, ProductDetailDto.class);
        return response.getBody().variacoes().get(0).id();
    }
}
