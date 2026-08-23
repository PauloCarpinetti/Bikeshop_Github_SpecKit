package com.bikeshop.admin;

import com.bikeshop.admin.dto.CreateProductRequest;
import com.bikeshop.admin.dto.CreateVariantRequest;
import com.bikeshop.admin.dto.UpdateProductRequest;
import com.bikeshop.admin.dto.UpdateVariantRequest;
import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.catalog.dto.VariantDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRUD de produtos e variações no backoffice (FR-001, FR-009, T074). Protegido por
 * {@code hasAnyRole("OPERATOR", "ADMIN")} em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
public class ProductAdminController {

    private final ProductAdminService productAdminService;

    public ProductAdminController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @GetMapping
    public List<ProductDetailDto> listar() {
        return productAdminService.listarProdutos();
    }

    @GetMapping("/{id}")
    public ProductDetailDto buscar(@PathVariable Long id) {
        return productAdminService.buscarProduto(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetailDto criar(@Valid @RequestBody CreateProductRequest request) {
        return productAdminService.criarProduto(request);
    }

    @PutMapping("/{id}")
    public ProductDetailDto atualizar(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return productAdminService.atualizarProduto(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable Long id) {
        productAdminService.inativarProduto(id);
    }

    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public VariantDto adicionarVariacao(@PathVariable Long id, @Valid @RequestBody CreateVariantRequest request) {
        return productAdminService.adicionarVariacao(id, request);
    }

    @PutMapping("/{id}/variants/{variantId}")
    public VariantDto atualizarVariacao(@PathVariable Long id, @PathVariable Long variantId,
                                         @Valid @RequestBody UpdateVariantRequest request) {
        return productAdminService.atualizarVariacao(id, variantId, request);
    }
}
