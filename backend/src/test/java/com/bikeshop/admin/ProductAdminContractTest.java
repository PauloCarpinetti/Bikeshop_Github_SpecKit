package com.bikeshop.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikeshop.admin.dto.CreateProductRequest;
import com.bikeshop.admin.dto.CreateVariantRequest;
import com.bikeshop.admin.dto.StockAdjustmentRequest;
import com.bikeshop.admin.dto.UpdateProductRequest;
import com.bikeshop.admin.dto.UpdateVariantRequest;
import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.catalog.dto.VariantDto;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.LoginRequest;
import com.bikeshop.customers.dto.RegisterRequest;
import java.math.BigDecimal;
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
class ProductAdminContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveGerenciarProdutoVariacoesEEstoqueComoAdmin() {
        HttpHeaders adminHeaders = loginComoAdmin();

        CreateVariantRequest variante1 = new CreateVariantRequest(
                "ADM-BIKE-M-" + UUID.randomUUID(), Map.of("tamanho", "M", "cor", "Preto"),
                new BigDecimal("1999.90"), 10,
                new BigDecimal("12.000"), new BigDecimal("20.00"), new BigDecimal("70.00"), new BigDecimal("120.00"));
        CreateProductRequest createRequest = new CreateProductRequest(
                "Bike Admin Teste", "Descrição de teste", "Bicicleta", "TestBrand", "Urbana",
                Map.of("material", "Aço"), Map.of(), List.of("https://placehold.co/1.png"), List.of(variante1));

        ResponseEntity<ProductDetailDto> createResponse = restTemplate.exchange(
                "/api/v1/admin/products", HttpMethod.POST, new HttpEntity<>(createRequest, adminHeaders), ProductDetailDto.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductDetailDto criado = createResponse.getBody();
        assertThat(criado.slug()).startsWith("bike-admin-teste");
        assertThat(criado.variacoes()).hasSize(1);
        Long produtoId = criado.id();
        String sku = criado.variacoes().get(0).sku();

        // Detalhe público reflete o produto recém-criado.
        ResponseEntity<ProductDetailDto> publicDetail = restTemplate.getForEntity(
                "/api/v1/catalog/products/" + criado.slug(), ProductDetailDto.class);
        assertThat(publicDetail.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Atualiza dados do produto.
        UpdateProductRequest updateRequest = new UpdateProductRequest(
                "Bike Admin Teste Atualizada", "Nova descrição", "Bicicleta", "TestBrand", "Urbana",
                Map.of("material", "Alumínio"), Map.of(), List.of("https://placehold.co/2.png"));
        ResponseEntity<ProductDetailDto> updateResponse = restTemplate.exchange(
                "/api/v1/admin/products/" + produtoId, HttpMethod.PUT, new HttpEntity<>(updateRequest, adminHeaders), ProductDetailDto.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody().nome()).isEqualTo("Bike Admin Teste Atualizada");

        // Adiciona uma segunda variação.
        CreateVariantRequest variante2 = new CreateVariantRequest(
                "ADM-BIKE-G-" + UUID.randomUUID(), Map.of("tamanho", "G", "cor", "Azul"),
                new BigDecimal("2099.90"), 5,
                new BigDecimal("12.500"), new BigDecimal("20.00"), new BigDecimal("70.00"), new BigDecimal("120.00"));
        ResponseEntity<VariantDto> addVariantResponse = restTemplate.exchange(
                "/api/v1/admin/products/" + produtoId + "/variants", HttpMethod.POST,
                new HttpEntity<>(variante2, adminHeaders), VariantDto.class);
        assertThat(addVariantResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long variacao2Id = addVariantResponse.getBody().id();

        // Atualiza a variação recém-criada (preço e status).
        UpdateVariantRequest updateVariantRequest = new UpdateVariantRequest(
                Map.of("tamanho", "G", "cor", "Azul"), new BigDecimal("1899.90"), "DISPONIVEL",
                new BigDecimal("12.500"), new BigDecimal("20.00"), new BigDecimal("70.00"), new BigDecimal("120.00"));
        ResponseEntity<VariantDto> updateVariantResponse = restTemplate.exchange(
                "/api/v1/admin/products/" + produtoId + "/variants/" + variacao2Id, HttpMethod.PUT,
                new HttpEntity<>(updateVariantRequest, adminHeaders), VariantDto.class);
        assertThat(updateVariantResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateVariantResponse.getBody().preco()).isEqualByComparingTo("1899.90");

        // Ajusta estoque da primeira variação (+7).
        ResponseEntity<VariantDto> stockResponse = restTemplate.exchange(
                "/api/v1/admin/products/" + sku + "/stock", HttpMethod.PATCH,
                new HttpEntity<>(new StockAdjustmentRequest(7, "Reposição de estoque"), adminHeaders), VariantDto.class);
        assertThat(stockResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(stockResponse.getBody().estoqueDisponivel()).isEqualTo(17);

        // Inativa o produto — some do catálogo público.
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/api/v1/admin/products/" + produtoId, HttpMethod.DELETE, new HttpEntity<>(adminHeaders), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> afterDelete = restTemplate.getForEntity(
                "/api/v1/catalog/products/" + criado.slug(), Map.class);
        assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deveRecusarAcessoAdministrativoParaClienteComum() {
        String email = "cliente-comum-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest("Cliente Comum", email, "senha12345");
        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity("/api/v1/auth/register", registerRequest, AuthResponse.class);
        HttpHeaders customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(registerResponse.getBody().accessToken());

        CreateProductRequest createRequest = new CreateProductRequest(
                "Produto Não Autorizado", null, "Bicicleta", null, null, Map.of(), Map.of(), List.of(),
                List.of(new CreateVariantRequest("NAO-AUTORIZADO", Map.of(), BigDecimal.TEN, 1,
                        BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/admin/products", HttpMethod.POST, new HttpEntity<>(createRequest, customerHeaders), Map.class);
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
