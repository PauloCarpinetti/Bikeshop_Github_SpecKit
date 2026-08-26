package com.bikeshop.admin;

import com.bikeshop.admin.dto.AuditLogDto;
import com.bikeshop.audit.AuditLog;
import com.bikeshop.audit.AuditLogRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta do Log de Auditoria pelo backoffice (FR-011, T081). Somente leitura — a gravação é
 * feita por {@link com.bikeshop.audit.AuditService} a cada ação administrativa sensível.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogAdminService {

    private static final int MAX_ENTRIES = 200;

    private final AuditLogRepository auditLogRepository;

    public AuditLogAdminService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public List<AuditLogDto> listar() {
        return auditLogRepository
                .findAll(PageRequest.of(0, MAX_ENTRIES, Sort.by(Sort.Direction.DESC, "occurredAt")))
                .map(this::toDto)
                .getContent();
    }

    private AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(log.getId(), log.getActor(), log.getActorRole(), log.getAction(), log.getEntityName(),
                log.getEntityId(), log.getPreviousState(), log.getNewState(), log.getOccurredAt());
    }
}
