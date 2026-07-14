package com.bikeshop.catalog;

/**
 * Filtros facetados suportados por {@code GET /catalog/products} (FR-003).
 */
public record ProductSearchFilters(
        String categoria,
        String marca,
        String modalidade,
        String tamanho,
        java.math.BigDecimal precoMin,
        java.math.BigDecimal precoMax
) {
}
