package com.bikeshop.catalog;

import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.catalog.dto.ProductSearchResultDto;
import com.bikeshop.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de catálogo (FR-002, FR-003). Ver contracts/api-overview.md.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class ProductController {

    private final ProductSearchService productSearchService;
    private final ProductRepository productRepository;
    private final VariacaoProdutoRepository variacaoProdutoRepository;
    private final ProductMapper productMapper;

    public ProductController(ProductSearchService productSearchService, ProductRepository productRepository,
                              VariacaoProdutoRepository variacaoProdutoRepository, ProductMapper productMapper) {
        this.productSearchService = productSearchService;
        this.productRepository = productRepository;
        this.variacaoProdutoRepository = variacaoProdutoRepository;
        this.productMapper = productMapper;
    }

    @GetMapping("/products")
    public ProductSearchResultDto search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String marca,
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) String tamanho,
            @RequestParam(required = false) BigDecimal precoMin,
            @RequestParam(required = false) BigDecimal precoMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ProductSearchFilters filters = new ProductSearchFilters(categoria, marca, modalidade, tamanho, precoMin, precoMax);
        return productSearchService.search(q, filters, page, Math.min(size, 100));
    }

    @GetMapping("/products/{slug}")
    public ProductDetailDto detail(@PathVariable String slug) {
        Produto produto = productRepository.findBySlugAndStatus(slug, ProdutoStatus.ATIVO)
                .orElseThrow(() -> new NotFoundException("Produto", slug));
        List<VariacaoProduto> variacoes = variacaoProdutoRepository.findByProdutoIdOrderByIdAsc(produto.getId());
        return productMapper.toDetail(produto, variacoes);
    }
}
