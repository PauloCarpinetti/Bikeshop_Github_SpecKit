package com.bikeshop.customers;

import com.bikeshop.orders.ReturnService;
import com.bikeshop.orders.dto.OrderDto;
import com.bikeshop.orders.dto.ReturnRequest;
import com.bikeshop.reviews.ReviewService;
import com.bikeshop.reviews.dto.CreateReviewRequest;
import com.bikeshop.reviews.dto.ReviewDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pós-venda do cliente autenticado (US2, FR-008): solicitação de troca/devolução e publicação de
 * avaliação.
 */
@RestController
@RequestMapping("/api/v1/account")
public class PostSaleController {

    private final ReturnService returnService;
    private final ReviewService reviewService;

    public PostSaleController(ReturnService returnService, ReviewService reviewService) {
        this.returnService = returnService;
        this.reviewService = reviewService;
    }

    @PostMapping("/orders/{orderId}/return")
    public OrderDto solicitarDevolucao(Authentication authentication, @PathVariable Long orderId,
                                        @Valid @RequestBody ReturnRequest request) {
        return returnService.solicitar(clienteId(authentication), orderId, request.motivo());
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto publicarAvaliacao(Authentication authentication, @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.publicar(clienteId(authentication), request);
    }

    private Long clienteId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
