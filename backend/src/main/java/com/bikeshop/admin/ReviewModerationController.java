package com.bikeshop.admin;

import com.bikeshop.reviews.dto.ModerateReviewRequest;
import com.bikeshop.reviews.dto.ReviewAdminDto;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Moderação de avaliações no backoffice (FR-009, T081b). Protegido por
 * {@code hasAnyRole("OPERATOR", "ADMIN")} em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/reviews")
public class ReviewModerationController {

    private final ReviewModerationService reviewModerationService;

    public ReviewModerationController(ReviewModerationService reviewModerationService) {
        this.reviewModerationService = reviewModerationService;
    }

    @GetMapping
    public List<ReviewAdminDto> listar() {
        return reviewModerationService.listar();
    }

    @PatchMapping("/{id}")
    public ReviewAdminDto moderar(@PathVariable Long id, @Valid @RequestBody ModerateReviewRequest request) {
        return reviewModerationService.moderar(id, request.aprovado());
    }
}
