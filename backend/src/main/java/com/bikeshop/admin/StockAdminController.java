package com.bikeshop.admin;

import com.bikeshop.admin.dto.StockAdjustmentRequest;
import com.bikeshop.catalog.dto.VariantDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ajuste manual de estoque no backoffice (FR-009, T075). Protegido por
 * {@code hasAnyRole("OPERATOR", "ADMIN")} em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
public class StockAdminController {

    private final ProductAdminService productAdminService;

    public StockAdminController(ProductAdminService productAdminService) {
        this.productAdminService = productAdminService;
    }

    @PatchMapping("/{sku}/stock")
    public VariantDto ajustarEstoque(@PathVariable String sku, @Valid @RequestBody StockAdjustmentRequest request) {
        return productAdminService.ajustarEstoque(sku, request);
    }
}
