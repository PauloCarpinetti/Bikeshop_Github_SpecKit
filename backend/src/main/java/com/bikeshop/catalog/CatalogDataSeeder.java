package com.bikeshop.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Popula o catálogo com produtos de exemplo em ambiente de desenvolvimento, já que o CRUD de
 * produtos pelo backoffice só é implementado na Fase 5. Não roda se já houver produtos cadastrados
 * (ex.: quando o backoffice passar a existir) nem no profile de teste.
 */
@Component
public class CatalogDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogDataSeeder.class);

    private final ProductRepository productRepository;
    private final VariacaoProdutoRepository variacaoProdutoRepository;
    private final ProductSearchService productSearchService;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    public CatalogDataSeeder(ProductRepository productRepository, VariacaoProdutoRepository variacaoProdutoRepository,
                              ProductSearchService productSearchService, ProductMapper productMapper,
                              ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.variacaoProdutoRepository = variacaoProdutoRepository;
        this.productSearchService = productSearchService;
        this.productMapper = productMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            log.info("Catálogo já possui produtos, seed de desenvolvimento ignorado.");
            return;
        }

        seedProduct(
                "Mountain Bike Aro 29 Explorer",
                "mountain-bike-aro-29-explorer",
                "Mountain bike para trilhas leves e uso urbano, quadro em alumínio.",
                "Bicicleta", "Explorer", "MTB",
                Map.of("material", "Alumínio", "freio", "A disco hidráulico", "marchas", 21),
                Map.of("standover", "780mm", "reach", "430mm", "stack", "600mm"),
                List.of("https://placehold.co/600x400?text=Explorer+MTB"),
                List.of(
                        variant("EXP-MTB-M-PRETO", Map.of("tamanho", "M", "cor", "Preto"), new BigDecimal("3299.90"), 8),
                        variant("EXP-MTB-G-AZUL", Map.of("tamanho", "G", "cor", "Azul"), new BigDecimal("3299.90"), 5)
                )
        );

        seedProduct(
                "Speed Aro 700 Veloce",
                "speed-aro-700-veloce",
                "Bicicleta speed para performance em asfalto, quadro em carbono.",
                "Bicicleta", "Veloce", "Speed",
                Map.of("material", "Carbono", "freio", "A disco", "marchas", 22),
                Map.of("standover", "760mm", "reach", "410mm", "stack", "560mm"),
                List.of("https://placehold.co/600x400?text=Veloce+Speed"),
                List.of(
                        variant("VEL-SPD-M-VERMELHO", Map.of("tamanho", "M", "cor", "Vermelho"), new BigDecimal("6499.00"), 4),
                        variant("VEL-SPD-P-PRETO", Map.of("tamanho", "P", "cor", "Preto"), new BigDecimal("6499.00"), 3)
                )
        );

        seedProduct(
                "Urbana Aro 26 CityRide",
                "urbana-aro-26-cityride",
                "Bicicleta urbana confortável para deslocamento diário na cidade.",
                "Bicicleta", "CityRide", "Urbana",
                Map.of("material", "Aço", "freio", "V-brake", "marchas", 7),
                Map.of("standover", "740mm", "reach", "400mm", "stack", "620mm"),
                List.of("https://placehold.co/600x400?text=CityRide+Urbana"),
                List.of(
                        variant("CR-URB-U-BRANCO", Map.of("tamanho", "Único", "cor", "Branco"), new BigDecimal("1599.90"), 12)
                )
        );

        seedProduct(
                "Capacete Ciclista ProSafe",
                "capacete-ciclista-prosafe",
                "Capacete leve e ventilado, certificado para ciclismo urbano e trilha.",
                "Acessório", "ProSafe", "Acessório",
                Map.of("material", "EPS + Policarbonato", "ventilacao", "18 entradas de ar"),
                Map.of(),
                List.of("https://placehold.co/600x400?text=Capacete+ProSafe"),
                List.of(
                        variant("PS-CAP-M-PRETO", Map.of("tamanho", "M", "cor", "Preto"), new BigDecimal("249.90"), 20),
                        variant("PS-CAP-G-PRETO", Map.of("tamanho", "G", "cor", "Preto"), new BigDecimal("249.90"), 15)
                )
        );

        log.info("Seed de catálogo concluído (produtos de exemplo criados e indexados no Meilisearch).");
    }

    private record VariantSeed(String sku, Map<String, Object> atributos, BigDecimal preco, int estoque) {
    }

    private VariantSeed variant(String sku, Map<String, Object> atributos, BigDecimal preco, int estoque) {
        return new VariantSeed(sku, atributos, preco, estoque);
    }

    private void seedProduct(String nome, String slug, String descricao, String categoria, String marca,
                              String modalidade, Map<String, Object> especificacoes, Map<String, Object> geometria,
                              List<String> imagens, List<VariantSeed> variantes) throws Exception {
        Produto produto = new Produto(
                nome, slug, descricao, categoria, marca, modalidade,
                objectMapper.writeValueAsString(especificacoes),
                objectMapper.writeValueAsString(geometria),
                objectMapper.writeValueAsString(imagens)
        );
        produto = productRepository.save(produto);

        List<VariacaoProduto> salvas = new java.util.ArrayList<>();
        for (VariantSeed v : variantes) {
            VariacaoProduto variacao = new VariacaoProduto(produto, v.sku(), objectMapper.writeValueAsString(v.atributos()), v.preco(), v.estoque());
            salvas.add(variacaoProdutoRepository.save(variacao));
        }

        productSearchService.indexProduct(toSearchDocument(produto, salvas, imagens));
    }

    private ProductSearchDocument toSearchDocument(Produto produto, List<VariacaoProduto> variacoes, List<String> imagens) {
        BigDecimal precoMinimo = variacoes.stream().map(VariacaoProduto::getPreco).min(java.util.Comparator.naturalOrder()).orElse(null);
        BigDecimal precoMaximo = variacoes.stream().map(VariacaoProduto::getPreco).max(java.util.Comparator.naturalOrder()).orElse(null);
        List<Map<String, Object>> atributos = variacoes.stream().map(v -> readAtributos(v.getAtributos())).toList();
        List<String> tamanhos = ProductSearchService.extractSizes(atributos);
        String imagemPrincipal = imagens.isEmpty() ? null : imagens.get(0);

        return new ProductSearchDocument(
                String.valueOf(produto.getId()), produto.getSlug(), produto.getNome(), produto.getDescricao(),
                produto.getCategoria(), produto.getMarca(), produto.getModalidade(),
                precoMinimo, precoMaximo, tamanhos, imagemPrincipal
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readAtributos(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
