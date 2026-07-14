package com.bikeshop.catalog;

import com.bikeshop.catalog.dto.ProductSearchResultDto;
import com.bikeshop.catalog.dto.ProductSummaryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.SearchResult;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Busca e indexação de catálogo via Meilisearch (research.md, seção 6; FR-003). O MySQL continua
 * sendo a fonte da verdade para o detalhe do produto; este serviço cuida apenas de busca/facetas.
 */
@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);
    private static final String INDEX_UID = "products";

    private final Client meilisearchClient;
    private final ObjectMapper objectMapper;

    public ProductSearchService(Client meilisearchClient, ObjectMapper objectMapper) {
        this.meilisearchClient = meilisearchClient;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void configureIndex() {
        try {
            Index index = meilisearchClient.index(INDEX_UID);
            index.updateFilterableAttributesSettings(new String[]{"categoria", "marca", "modalidade", "tamanhos", "precoMinimo"});
            index.updateSearchableAttributesSettings(new String[]{"nome", "descricao", "marca", "categoria"});
            index.updateSortableAttributesSettings(new String[]{"precoMinimo"});
        } catch (MeilisearchException ex) {
            log.warn("Não foi possível configurar o índice '{}' do Meilisearch na inicialização: {}", INDEX_UID, ex.getMessage());
        }
    }

    public void indexProduct(ProductSearchDocument document) {
        try {
            String json = objectMapper.writeValueAsString(List.of(document));
            meilisearchClient.index(INDEX_UID).addDocuments(json, "id");
        } catch (Exception ex) {
            log.warn("Falha ao indexar produto '{}' no Meilisearch: {}", document.getId(), ex.getMessage());
        }
    }

    public ProductSearchResultDto search(String q, ProductSearchFilters filters, int page, int size) {
        try {
            Index index = meilisearchClient.index(INDEX_UID);
            SearchRequest request = new SearchRequest(q == null ? "" : q)
                    .setLimit(size)
                    .setOffset(page * size);

            List<String> filterExpressions = buildFilterExpressions(filters);
            if (!filterExpressions.isEmpty()) {
                request.setFilter(filterExpressions.toArray(new String[0]));
            }

            SearchResult result = (SearchResult) index.search(request);
            List<ProductSummaryDto> items = result.getHits().stream()
                    .map(this::toSummary)
                    .toList();

            return new ProductSearchResultDto(items, result.getEstimatedTotalHits(), page, size);
        } catch (MeilisearchException ex) {
            log.error("Falha ao consultar o Meilisearch: {}", ex.getMessage());
            return new ProductSearchResultDto(List.of(), 0, page, size);
        }
    }

    private List<String> buildFilterExpressions(ProductSearchFilters filters) {
        List<String> expressions = new ArrayList<>();
        if (filters == null) {
            return expressions;
        }
        if (filters.categoria() != null && !filters.categoria().isBlank()) {
            expressions.add("categoria = '%s'".formatted(escape(filters.categoria())));
        }
        if (filters.marca() != null && !filters.marca().isBlank()) {
            expressions.add("marca = '%s'".formatted(escape(filters.marca())));
        }
        if (filters.modalidade() != null && !filters.modalidade().isBlank()) {
            expressions.add("modalidade = '%s'".formatted(escape(filters.modalidade())));
        }
        if (filters.tamanho() != null && !filters.tamanho().isBlank()) {
            expressions.add("tamanhos = '%s'".formatted(escape(filters.tamanho())));
        }
        if (filters.precoMin() != null) {
            expressions.add("precoMinimo >= %s".formatted(filters.precoMin()));
        }
        if (filters.precoMax() != null) {
            expressions.add("precoMinimo <= %s".formatted(filters.precoMax()));
        }
        return expressions;
    }

    private String escape(String value) {
        return value.replace("'", "\\'");
    }

    @SuppressWarnings("unchecked")
    private ProductSummaryDto toSummary(HashMap<String, Object> hit) {
        return new ProductSummaryDto(
                Long.valueOf((String) hit.get("id")),
                (String) hit.get("slug"),
                (String) hit.get("nome"),
                (String) hit.get("categoria"),
                (String) hit.get("marca"),
                (String) hit.get("modalidade"),
                toBigDecimal(hit.get("precoMinimo")),
                toBigDecimal(hit.get("precoMaximo")),
                (String) hit.get("imagemPrincipal")
        );
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value.toString());
    }

    /** Extrai os valores distintos de "tamanho" das variações, para facetar por tamanho. */
    public static List<String> extractSizes(List<Map<String, Object>> atributosPorVariacao) {
        return atributosPorVariacao.stream()
                .map(atributos -> atributos.get("tamanho"))
                .filter(java.util.Objects::nonNull)
                .map(Object::toString)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
