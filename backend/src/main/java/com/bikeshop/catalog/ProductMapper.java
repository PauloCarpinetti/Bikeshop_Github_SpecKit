package com.bikeshop.catalog;

import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.catalog.dto.ProductSummaryDto;
import com.bikeshop.catalog.dto.VariantDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Mapeamento entidade -> DTO para o catálogo. Os campos JSON (String pré-serializada, ver
 * {@link Produto}) são desserializados aqui para as estruturas expostas na API.
 */
@Component
public class ProductMapper {

    @Autowired
    private ObjectMapper objectMapper;

    public ProductSummaryDto toSummary(Produto produto, List<VariacaoProduto> variacoes) {
        BigDecimal precoMinimo = variacoes.stream().map(VariacaoProduto::getPreco).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal precoMaximo = variacoes.stream().map(VariacaoProduto::getPreco).max(Comparator.naturalOrder()).orElse(null);
        List<String> imagens = readList(produto.getImagens());
        String imagemPrincipal = imagens.isEmpty() ? null : imagens.get(0);

        return new ProductSummaryDto(
                produto.getId(),
                produto.getSlug(),
                produto.getNome(),
                produto.getCategoria(),
                produto.getMarca(),
                produto.getModalidade(),
                precoMinimo,
                precoMaximo,
                imagemPrincipal
        );
    }

    public ProductDetailDto toDetail(Produto produto, List<VariacaoProduto> variacoes) {
        List<VariantDto> variantDtos = variacoes.stream().map(this::toVariant).toList();

        return new ProductDetailDto(
                produto.getId(),
                produto.getSlug(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getCategoria(),
                produto.getMarca(),
                produto.getModalidade(),
                readMap(produto.getEspecificacoesTecnicas()),
                readMap(produto.getTabelaGeometria()),
                readList(produto.getImagens()),
                produto.getStatus().name(),
                variantDtos
        );
    }

    public VariantDto toVariant(VariacaoProduto variacao) {
        return new VariantDto(
                variacao.getId(),
                variacao.getSku(),
                readMap(variacao.getAtributos()),
                variacao.getPreco(),
                variacao.getEstoqueDisponivel(),
                variacao.getStatus().name()
        );
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }
}
