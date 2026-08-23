package com.bikeshop.customers;

import com.bikeshop.cart.CartCookieResolver;
import com.bikeshop.cart.CartService;
import com.bikeshop.common.exception.BusinessException;
import com.bikeshop.common.security.JwtService;
import com.bikeshop.customers.dto.AuthResponse;
import com.bikeshop.customers.dto.LoginRequest;
import com.bikeshop.customers.dto.RefreshRequest;
import com.bikeshop.customers.dto.RegisterRequest;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro, login e refresh de token (FR-008). Registro e login mesclam automaticamente o
 * carrinho de visitante da sessão atual com o carrinho salvo do cliente (FR-004, T040).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CartService cartService;
    private final CartCookieResolver cartCookieResolver;

    public AuthController(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder,
                           JwtService jwtService, CartService cartService, CartCookieResolver cartCookieResolver) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.cartService = cartService;
        this.cartCookieResolver = cartCookieResolver;
    }

    @PostMapping("/register")
    @Transactional
    public AuthResponse register(@Valid @RequestBody RegisterRequest request,
                                  @CookieValue(name = CartCookieResolver.CART_COOKIE, required = false) String cartId,
                                  HttpServletResponse response) {
        if (clienteRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_EM_USO", "Já existe uma conta com este e-mail", HttpStatus.CONFLICT);
        }

        Cliente cliente = new Cliente(request.nome(), request.email(), passwordEncoder.encode(request.senha()));
        cliente = clienteRepository.save(cliente);

        mergeCart(cartId, cliente.getId(), response);
        return toAuthResponse(cliente);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request,
                               @CookieValue(name = CartCookieResolver.CART_COOKIE, required = false) String cartId,
                               HttpServletResponse response) {
        Cliente cliente = clienteRepository.findByEmail(request.email())
                .filter(c -> passwordEncoder.matches(request.senha(), c.getSenhaHash()))
                .orElseThrow(() -> new BusinessException("CREDENCIAIS_INVALIDAS", "E-mail ou senha inválidos", HttpStatus.UNAUTHORIZED));

        mergeCart(cartId, cliente.getId(), response);
        return toAuthResponse(cliente);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        if (!jwtService.isValid(request.refreshToken())) {
            throw new BusinessException("TOKEN_INVALIDO", "Refresh token inválido ou expirado", HttpStatus.UNAUTHORIZED);
        }

        Claims claims = jwtService.parseClaims(request.refreshToken());
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new BusinessException("TOKEN_INVALIDO", "Token informado não é um refresh token", HttpStatus.UNAUTHORIZED);
        }

        Long clienteId = Long.valueOf(claims.getSubject());
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new BusinessException("TOKEN_INVALIDO", "Cliente não encontrado", HttpStatus.UNAUTHORIZED));
        return toAuthResponse(cliente);
    }

    private void mergeCart(String cartId, Long clienteId, HttpServletResponse response) {
        String resolvedCartId = cartCookieResolver.resolve(cartId, response);
        cartService.mergeIntoCustomerCart(resolvedCartId, clienteId);
    }

    private AuthResponse toAuthResponse(Cliente cliente) {
        String accessToken = jwtService.generateAccessToken(String.valueOf(cliente.getId()), List.of(cliente.getRole()));
        String refreshToken = jwtService.generateRefreshToken(String.valueOf(cliente.getId()));
        return new AuthResponse(accessToken, refreshToken, cliente.getId(), cliente.getNome(), cliente.getEmail());
    }
}
