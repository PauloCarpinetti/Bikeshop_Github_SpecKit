package com.bikeshop.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.catalog.dto.ProductDetailDto;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveListarProdutosComFormatoDeContratoEsperado() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/catalog/products?page=0&size=10", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("items", "total", "page", "size");
    }

    @Test
    void deveRetornarDetalheDoProdutoSeedadoPorSlug() {
        ResponseEntity<ProductDetailDto> response =
                restTemplate.getForEntity("/api/v1/catalog/products/mountain-bike-aro-29-explorer", ProductDetailDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().nome()).isEqualTo("Mountain Bike Aro 29 Explorer");
        assertThat(response.getBody().variacoes()).isNotEmpty();
    }

    @Test
    void deveRetornar404ParaSlugInexistente() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/v1/catalog/products/produto-que-nao-existe", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
