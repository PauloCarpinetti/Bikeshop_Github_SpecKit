package com.bikeshop.admin;

import com.bikeshop.admin.dto.AuditLogDto;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta do Log de Auditoria no backoffice (FR-011, T081). Protegido por
 * {@code hasAnyRole("OPERATOR", "ADMIN")} em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {

    private final AuditLogAdminService auditLogAdminService;

    public AuditLogController(AuditLogAdminService auditLogAdminService) {
        this.auditLogAdminService = auditLogAdminService;
    }

    @GetMapping
    public List<AuditLogDto> listar() {
        return auditLogAdminService.listar();
    }
}
