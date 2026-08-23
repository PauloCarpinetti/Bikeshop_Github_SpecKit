package com.bikeshop.cart;

import com.bikeshop.cart.dto.AddCartItemRequest;
import com.bikeshop.cart.dto.CartViewDto;
import com.bikeshop.cart.dto.UpdateCartItemRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de carrinho (FR-004). O carrinho de visitante é identificado por um cookie próprio
 * ({@value CartCookieResolver#CART_COOKIE}), emitido automaticamente na primeira chamada.
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private final CartService cartService;
    private final CartCookieResolver cartCookieResolver;

    public CartController(CartService cartService, CartCookieResolver cartCookieResolver) {
        this.cartService = cartService;
        this.cartCookieResolver = cartCookieResolver;
    }

    @GetMapping
    public CartViewDto getCart(@CookieValue(name = CartCookieResolver.CART_COOKIE, required = false) String cartId,
                                HttpServletResponse response) {
        return cartService.getCartView(cartCookieResolver.resolve(cartId, response));
    }

    @PostMapping("/items")
    public CartViewDto addItem(@CookieValue(name = CartCookieResolver.CART_COOKIE, required = false) String cartId,
                                HttpServletResponse response,
                                @Valid @RequestBody AddCartItemRequest request) {
        String resolvedId = cartCookieResolver.resolve(cartId, response);
        return cartService.addItem(resolvedId, request.variacaoProdutoId(), request.quantidade());
    }

    @PatchMapping("/items/{itemId}")
    public CartViewDto updateItem(@CookieValue(name = CartCookieResolver.CART_COOKIE, required = false) String cartId,
                                   HttpServletResponse response,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        String resolvedId = cartCookieResolver.resolve(cartId, response);
        return cartService.updateItem(resolvedId, itemId, request.quantidade());
    }

    @DeleteMapping("/items/{itemId}")
    public CartViewDto removeItem(@CookieValue(name = CartCookieResolver.CART_COOKIE, required = false) String cartId,
                                   HttpServletResponse response,
                                   @PathVariable Long itemId) {
        String resolvedId = cartCookieResolver.resolve(cartId, response);
        return cartService.removeItem(resolvedId, itemId);
    }
}
