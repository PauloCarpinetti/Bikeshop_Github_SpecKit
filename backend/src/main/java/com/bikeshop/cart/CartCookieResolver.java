package com.bikeshop.cart;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolve (ou emite) o cookie que identifica o carrinho de visitante. Compartilhado entre
 * {@code CartController} e {@code CheckoutController}, já que o checkout também precisa do
 * carrinho atual.
 */
@Component
public class CartCookieResolver {

    public static final String CART_COOKIE = "bikeshop_cart_id";
    private static final int CART_COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 30;

    public String resolve(String cartId, HttpServletResponse response) {
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
