package com.bikeshop.checkout;

import com.bikeshop.cart.CartCookieResolver;
import com.bikeshop.checkout.dto.CheckoutResultDto;
import com.bikeshop.checkout.dto.CreateOrderRequest;
import com.bikeshop.checkout.dto.ShippingQuoteRequest;
import com.bikeshop.checkout.dto.ShippingQuoteResponseDto;
import com.bikeshop.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Orquestração de checkout (FR-005, FR-006, FR-007). Ver contracts/api-overview.md.
 */
@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final CartCookieResolver cartCookieResolver;

    public CheckoutController(CheckoutService checkoutService, CartCookieResolver cartCookieResolver) {
        this.checkoutService = checkoutService;
        this.cartCookieResolver = cartCookieResolver;
    }

    @PostMapping("/shipping-quote")
    public ShippingQuoteResponseDto shippingQuote(
            @CookieValue(name = CartCookieResolver.CART_COOKIE, required = false) String cartId,
            HttpServletResponse response,
            @Valid @RequestBody ShippingQuoteRequest request) {
        String resolvedCartId = requireExistingCart(cartId, response);
        return checkoutService.quoteShipping(resolvedCartId, request.cep());
    }

    @PostMapping("/orders")
    public CheckoutResultDto criarPedido(
            @CookieValue(name = CartCookieResolver.CART_COOKIE, required = false) String cartId,
            HttpServletResponse response,
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        String resolvedCartId = requireExistingCart(cartId, response);
        return checkoutService.criarPedido(resolvedCartId, request, resolveClienteId(authentication));
    }

    /** Frete/checkout exigem um carrinho já existente — diferente do CartController, não cria um novo aqui. */
    private String requireExistingCart(String cartId, HttpServletResponse response) {
        if (cartId == null || cartId.isBlank()) {
            throw new BusinessException("CARRINHO_VAZIO", "Nenhum carrinho encontrado. Adicione itens antes de continuar.", HttpStatus.BAD_REQUEST);
        }
        return cartCookieResolver.resolve(cartId, response);
    }

    /** Checkout é público (guest checkout) — o cliente_id só é vinculado ao pedido se houver JWT válido (T056). */
    private Long resolveClienteId(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return Long.valueOf(authentication.getName());
    }
}
