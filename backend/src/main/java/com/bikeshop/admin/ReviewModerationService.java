package com.bikeshop.admin;

import com.bikeshop.audit.AuditService;
import com.bikeshop.reviews.ReviewService;
import com.bikeshop.reviews.dto.ReviewAdminDto;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moderação de avaliações no backoffice (FR-009, T081b): aprova ou rejeita uma avaliação
 * publicada por cliente. Reaproveita {@link ReviewService} para leitura/mutação e registra a
 * ação no Log de Auditoria (FR-011), mesmo padrão de {@link OrderAdminService}.
 */
@Service
@Transactional
public class ReviewModerationService {

    private final ReviewService reviewService;
    private final AuditService auditService;

    public ReviewModerationService(ReviewService reviewService, AuditService auditService) {
        this.reviewService = reviewService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ReviewAdminDto> listar() {
        return reviewService.listarParaModeracao();
    }

    public ReviewAdminDto moderar(Long id, boolean aprovado) {
        String statusAnterior = reviewService.statusAtual(id);
        ReviewAdminDto atualizado = reviewService.moderar(id, aprovado);

        auditService.record("MODERAR_AVALIACAO", "Avaliacao", String.valueOf(id),
                Map.of("status", statusAnterior), Map.of("status", atualizado.status()));

        return atualizado;
    }
}
