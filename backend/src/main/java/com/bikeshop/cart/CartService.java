package com.bikeshop.cart;

import com.bikeshop.cart.dto.CartItemViewDto;
import com.bikeshop.cart.dto.CartViewDto;
import com.bikeshop.catalog.Produto;
import com.bikeshop.catalog.VariacaoProduto;
import com.bikeshop.catalog.VariacaoProdutoRepository;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.exception.NotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negócio do carrinho (FR-004). Nesta sub-fase (3A), trabalha exclusivamente com o
 * carrinho de visitante (Redis); merge para conta autenticada é implementado na sub-fase 3C.
 *
 * <p>{@code @Transactional} mantém a sessão Hibernate aberta durante o enriquecimento do carrinho
 * com dados de {@code Produto}/{@code VariacaoProduto} (associação lazy), já que a aplicação roda
 * com {@code open-in-view: false}.
 */
@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartRedisRepository cartRepository;
    private final VariacaoProdutoRepository variacaoProdutoRepository;
    private final ObjectMapper objectMapper;

    public CartService(CartRedisRepository cartRepository, VariacaoProdutoRepository variacaoProdutoRepository,
                        ObjectMapper objectMapper) {
        this.cartRepository = cartRepository;
        this.variacaoProdutoRepository = variacaoProdutoRepository;
        this.objectMapper = objectMapper;
    }

    public CartViewDto getCartView(String cartId) {
        return toView(cartRepository.findOrCreate(cartId));
    }

    public CartViewDto addItem(String cartId, Long variacaoProdutoId, int quantidade) {
        VariacaoProduto variacao = variacaoProdutoRepository.findById(variacaoProdutoId)
                .orElseThrow(() -> new NotFoundException("Variação de produto", variacaoProdutoId));

        Carrinho cart = cartRepository.findOrCreate(cartId);
        int quantidadeAtual = cart.findItem(variacaoProdutoId).map(ItemCarrinho::getQuantidade).orElse(0);
        int novaQuantidade = quantidadeAtual + quantidade;
        validarEstoque(variacao, novaQuantidade);

        cart.upsertItem(variacaoProdutoId, novaQuantidade);
        cartRepository.save(cart);
        return toView(cart);
    }

    public CartViewDto updateItem(String cartId, Long variacaoProdutoId, int quantidade) {
        Carrinho cart = cartRepository.findOrCreate(cartId);
        if (cart.findItem(variacaoProdutoId).isEmpty()) {
            throw new NotFoundException("Item de carrinho", variacaoProdutoId);
        }

        if (quantidade <= 0) {
            cart.removeItem(variacaoProdutoId);
        } else {
            VariacaoProduto variacao = variacaoProdutoRepository.findById(variacaoProdutoId)
                    .orElseThrow(() -> new NotFoundException("Variação de produto", variacaoProdutoId));
            validarEstoque(variacao, quantidade);
            cart.upsertItem(variacaoProdutoId, quantidade);
        }

        cartRepository.save(cart);
        return toView(cart);
    }

    public CartViewDto removeItem(String cartId, Long variacaoProdutoId) {
        Carrinho cart = cartRepository.findOrCreate(cartId);
        cart.removeItem(variacaoProdutoId);
        cartRepository.save(cart);
        return toView(cart);
    }

    private void validarEstoque(VariacaoProduto variacao, int quantidadeSolicitada) {
        if (quantidadeSolicitada > variacao.getEstoqueDisponivel()) {
            throw new BusinessException(
                    "ESTOQUE_INSUFICIENTE",
                    "Quantidade solicitada (%d) excede o estoque disponível (%d) para o SKU %s"
                            .formatted(quantidadeSolicitada, variacao.getEstoqueDisponivel(), variacao.getSku()),
                    HttpStatus.CONFLICT
            );
        }
    }

    private CartViewDto toView(Carrinho cart) {
        List<CartItemViewDto> itens = cart.getItens().stream()
                .map(this::toItemView)
                .filter(java.util.Objects::nonNull)
                .toList();
        BigDecimal total = itens.stream().map(CartItemViewDto::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartViewDto(cart.getId(), itens, total);
    }

    private CartItemViewDto toItemView(ItemCarrinho item) {
        return variacaoProdutoRepository.findById(item.getVariacaoProdutoId())
                .map(variacao -> {
                    Produto produto = variacao.getProduto();
                    List<String> imagens = readImagens(produto.getImagens());
                    BigDecimal subtotal = variacao.getPreco().multiply(BigDecimal.valueOf(item.getQuantidade()));
                    return new CartItemViewDto(
                            variacao.getId(),
                            variacao.getSku(),
                            produto.getNome(),
                            produto.getSlug(),
                            imagens.isEmpty() ? null : imagens.get(0),
                            variacao.getPreco(),
                            item.getQuantidade(),
                            subtotal
                    );
                })
                .orElse(null);
    }

    private List<String> readImagens(String json) {
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
