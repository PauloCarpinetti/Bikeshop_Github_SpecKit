package com.bikeshop.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Ponto único de gravação do log de auditoria. Qualquer módulo que execute uma ação administrativa
 * sensível (backoffice) MUST chamar este serviço (FR-011).
 */
@Service
public class AuditService {

  private static final Logger log = LoggerFactory.getLogger(AuditService.class);

  private final AuditLogRepository repository;
  private final ObjectMapper objectMapper;

  public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
  }

  public void record(
      String action, String entityName, String entityId, Object previousState, Object newState) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String actor =
        authentication != null ? String.valueOf(authentication.getPrincipal()) : "system";
    String actorRole =
        authentication == null
            ? "SYSTEM"
            : authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("UNKNOWN");

    AuditLog entry =
        new AuditLog(
            actor,
            actorRole,
            action,
            entityName,
            entityId,
            toJson(previousState),
            toJson(newState),
            null);

    repository.save(entry);
    log.info("audit action={} entity={} id={} actor={}", action, entityName, entityId, actor);
  }

  private String toJson(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      log.warn("Falha ao serializar estado para auditoria: {}", ex.getMessage());
      return null;
    }
  }
}
