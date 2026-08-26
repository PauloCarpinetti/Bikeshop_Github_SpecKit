package com.bikeshop.admin.dto;

import java.time.Instant;

public record AuditLogDto(Long id, String actor, String actorRole, String action, String entityName,
                           String entityId, String previousState, String newState, Instant occurredAt) {
}
