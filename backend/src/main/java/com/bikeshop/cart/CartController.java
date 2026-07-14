package com.bikeshop.cart;

import com.bikeshop.cart.dto.AddCartItemRequest;
import com.bikeshop.cart.dto.CartViewDto;
import com.bikeshop.cart.dto.UpdateCartItemRequest;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
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
 * ({@value #CART_COOKIE}), emitido automaticamente na primeira chamada.
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    static final String CART_COOKIE = "bikeshop_cart_id";
    private static final int CART_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartViewDto getCart(@CookieValue(name = CART_COOKIE, required = false) String cartId,
                                HttpServletResponse response) {
        return cartService.getCartView(resolveCartId(cartId, response));
    }

    @PostMapping("/items")
    public CartViewDto addItem(@CookieValue(name = CART_COOKIE, required = false) String cartId,
                                HttpServletResponse response,
                                @Valid @RequestBody AddCartItemRequest request) {
        String resolvedId = resolveCartId(cartId, response);
        return cartService.addItem(resolvedId, request.variacaoProdutoId(), request.quantidade());
    }

    @PatchMapping("/items/{itemId}")
    public CartViewDto updateItem(@CookieValue(name = CART_COOKIE, required = false) String cartId,
                                   HttpServletResponse response,
                                   @PathVariable Long itemId,
                                   @Valid @RequestBody UpdateCartItemRequest request) {
        String resolvedId = resolveCartId(cartId, response);
        return cartService.updateItem(resolvedId, itemId, request.quantidade());
    }

    @DeleteMapping("/items/{itemId}")
    public CartViewDto removeItem(@CookieValue(name = CART_COOKIE, required = false) String cartId,
                                   HttpServletResponse response,
                                   @PathVariable Long itemId) {
        String resolvedId = resolveCartId(cartId, response);
        return cartService.removeItem(resolvedId, itemId);
    }

    private String resolveCartId(String cartId, HttpServletResponse response) {
        if (cartId != null && !cartId.isBlank()) {
            return cartId;
        }
        String newId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(CART_COOKIE, newId);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(CART_COOKIE_MAX_AGE_SECONDS);
        response.addCookie(cookie);
        return newId;
    }
}
