package com.bikeshop.admin;

import com.bikeshop.admin.dto.CreateProductRequest;
import com.bikeshop.admin.dto.CreateVariantRequest;
import com.bikeshop.admin.dto.UpdateProductRequest;
import com.bikeshop.admin.dto.UpdateVariantRequest;
import com.bikeshop.admin.dto.StockAdjustmentRequest;
import com.bikeshop.catalog.InventoryAdjustedEvent;
import com.bikeshop.catalog.ProdutoStatus;
import com.bikeshop.catalog.ProductMapper;
import com.bikeshop.catalog.ProductRepository;
import com.bikeshop.catalog.ProductSearchDocument;
import com.bikeshop.catalog.ProductSearchService;
import com.bikeshop.catalog.Produto;
import com.bikeshop.catalog.VariacaoProduto;
import com.bikeshop.catalog.VariacaoProdutoRepository;
import com.bikeshop.catalog.VariacaoStatus;
import com.bikeshop.catalog.dto.ProductDetailDto;
import com.bikeshop.catalog.dto.VariantDto;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.exception.NotFoundException;
import com.bikeshop.common.messaging.DomainEventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de produto/variação pelo backoffice (FR-001, FR-009, T073). Mantém o índice de busca
 * (Meilisearch) sincronizado a cada mutação — mesma lógica de indexação usada por
 * {@link com.bikeshop.catalog.CatalogDataSeeder}, agora também acionada aqui.
 */
@Service
@Transactional
public class ProductAdminService {

    private final ProductRepository productRepository;
    private final VariacaoProdutoRepository variacaoProdutoRepository;
    private final ProductSearchService productSearchService;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher eventPublisher;

    public ProductAdminService(ProductRepository productRepository, VariacaoProdutoRepository variacaoProdutoRepository,
                                ProductSearchService productSearchService, ProductMapper productMapper,
                                ObjectMapper objectMapper, DomainEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.variacaoProdutoRepository = variacaoProdutoRepository;
        this.productSearchService = productSearchService;
        this.productMapper = productMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<ProductDetailDto> listarProdutos() {
        return productRepository.findAll().stream()
                .map(produto -> productMapper.toDetail(produto, variacaoProdutoRepository.findByProdutoIdOrderByIdAsc(produto.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailDto buscarProduto(Long id) {
        Produto produto = requireProduto(id);
        return productMapper.toDetail(produto, variacaoProdutoRepository.findByProdutoIdOrderByIdAsc(id));
    }

    public ProductDetailDto criarProduto(CreateProductRequest request) {
        String slug = gerarSlugUnico(request.nome());
        Produto produto = new Produto(request.nome(), slug, request.descricao(), request.categoria(),
                request.marca(), request.modalidade(), toJson(request.especificacoesTecnicas()),
                toJson(request.tabelaGeometria()), toJson(imagensOuVazio(request.imagens())));
        produto = productRepository.save(produto);

        List<VariacaoProduto> variacoes = new ArrayList<>();
        for (CreateVariantRequest v : request.variantes()) {
            variacoes.add(salvarNovaVariacao(produto, v));
        }

        reindexar(produto, variacoes);
        return productMapper.toDetail(produto, variacoes);
    }

    public ProductDetailDto atualizarProduto(Long id, UpdateProductRequest request) {
        Produto produto = requireProduto(id);
        produto.atualizar(request.nome(), request.descricao(), request.categoria(), request.marca(), request.modalidade(),
                toJson(request.especificacoesTecnicas()), toJson(request.tabelaGeometria()), toJson(imagensOuVazio(request.imagens())));

        List<VariacaoProduto> variacoes = variacaoProdutoRepository.findByProdutoIdOrderByIdAsc(id);
        reindexar(produto, variacoes);
        return productMapper.toDetail(produto, variacoes);
    }

    /** Soft delete (FR-009): inativa em vez de apagar, preservando o histórico de pedidos que referenciam o produto. */
    public void inativarProduto(Long id) {
        Produto produto = requireProduto(id);
        produto.atualizarStatus(ProdutoStatus.INATIVO);
        productSearchService.removeProduct(id);
    }

    public VariantDto adicionarVariacao(Long produtoId, CreateVariantRequest request) {
        Produto produto = requireProduto(produtoId);
        VariacaoProduto variacao = salvarNovaVariacao(produto, request);

        List<VariacaoProduto> variacoes = variacaoProdutoRepository.findByProdutoIdOrderByIdAsc(produtoId);
        reindexar(produto, variacoes);
        return productMapper.toVariant(variacao);
    }

    public VariantDto atualizarVariacao(Long produtoId, Long variacaoId, UpdateVariantRequest request) {
        VariacaoProduto variacao = variacaoProdutoRepository.findById(variacaoId)
                .filter(v -> v.getProduto().getId().equals(produtoId))
                .orElseThrow(() -> new NotFoundException("Variação de produto", variacaoId));
        variacao.atualizar(toJson(request.atributos()), request.preco(), VariacaoStatus.valueOf(request.status()),
                request.pesoKg(), request.alturaCm(), request.larguraCm(), request.comprimentoCm());

        Produto produto = requireProduto(produtoId);
        List<VariacaoProduto> variacoes = variacaoProdutoRepository.findByProdutoIdOrderByIdAsc(produtoId);
        reindexar(produto, variacoes);
        return productMapper.toVariant(variacao);
    }

    /** Ajuste manual de estoque (FR-009, T075): positivo repõe, negativo corrige/remove. Publica
     * o evento já usado pelo checkout ({@code inventory.adjusted}), sem consumidor hoje. */
    public VariantDto ajustarEstoque(String sku, StockAdjustmentRequest request) {
        VariacaoProduto variacao = variacaoProdutoRepository.findBySku(sku)
                .orElseThrow(() -> new NotFoundException("Variação de produto", sku));
        variacao.ajustarEstoque(request.ajuste());

        eventPublisher.publish("inventory.adjusted", new InventoryAdjustedEvent(
                variacao.getId(), variacao.getSku(), -request.ajuste(), variacao.getEstoqueDisponivel()));

        Produto produto = variacao.getProduto();
        List<VariacaoProduto> variacoes = variacaoProdutoRepository.findByProdutoIdOrderByIdAsc(produto.getId());
        reindexar(produto, variacoes);
        return productMapper.toVariant(variacao);
    }

    private VariacaoProduto salvarNovaVariacao(Produto produto, CreateVariantRequest request) {
        if (variacaoProdutoRepository.findBySku(request.sku()).isPresent()) {
            throw new BusinessException("SKU_EM_USO", "Já existe uma variação com o SKU %s".formatted(request.sku()),
                    HttpStatus.CONFLICT);
        }
        VariacaoProduto variacao = new VariacaoProduto(produto, request.sku(), toJson(request.atributos()),
                request.preco(), request.estoqueDisponivel(), request.pesoKg(), request.alturaCm(),
                request.larguraCm(), request.comprimentoCm());
        return variacaoProdutoRepository.save(variacao);
    }

    private Produto requireProduto(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Produto", id));
    }

    private void reindexar(Produto produto, List<VariacaoProduto> variacoes) {
        if (produto.getStatus() != ProdutoStatus.ATIVO || variacoes.isEmpty()) {
            // FR-001: sem variação ativa (ou produto inativo), não fica disponível para venda/busca.
            productSearchService.removeProduct(produto.getId());
            return;
        }

        BigDecimal precoMinimo = variacoes.stream().map(VariacaoProduto::getPreco).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal precoMaximo = variacoes.stream().map(VariacaoProduto::getPreco).max(Comparator.naturalOrder()).orElse(null);
        List<Map<String, Object>> atributos = variacoes.stream().map(v -> readMap(v.getAtributos())).toList();
        List<String> tamanhos = ProductSearchService.extractSizes(atributos);
        List<String> imagens = readList(produto.getImagens());
        String imagemPrincipal = imagens.isEmpty() ? null : imagens.get(0);

        productSearchService.indexProduct(new ProductSearchDocument(
                String.valueOf(produto.getId()), produto.getSlug(), produto.getNome(), produto.getDescricao(),
                produto.getCategoria(), produto.getMarca(), produto.getModalidade(),
                precoMinimo, precoMaximo, tamanhos, imagemPrincipal
        ));
    }

    private String gerarSlugUnico(String nome) {
        String base = slugify(nome);
        String slug = base;
        int sufixo = 2;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + sufixo++;
        }
        return slug;
    }

    private String slugify(String valor) {
        String semAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = semAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.isBlank() ? "produto" : slug;
    }

    private List<String> imagensOuVazio(List<String> imagens) {
        return imagens == null ? List.of() : imagens;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao serializar dados do produto", ex);
        }
    }

    @SuppressWarnings("unchecked")
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
