package com.bikeshop.catalog.dto;

import java.util.List;

public record ProductSearchResultDto(
        List<ProductSummaryDto> items,
        long total,
        int page,
        int size
) {
}
