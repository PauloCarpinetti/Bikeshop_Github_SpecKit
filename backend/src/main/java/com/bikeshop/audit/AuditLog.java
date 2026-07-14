package com.bikeshop.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Registro imutável de ação administrativa sensível (FR-011, Princípio III). Somente inserção: não
 * há update/delete expostos para esta entidade.
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String actor;

  @Column(name = "actor_role", nullable = false)
  private String actorRole;

  @Column(nullable = false)
  private String action;

  @Column(name = "entity_name", nullable = false)
  private String entityName;

  @Column(name = "entity_id", nullable = false)
  private String entityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "previous_state")
  private String previousState;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "new_state")
  private String newState;

  private String origin;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt = Instant.now();

  protected AuditLog() {
    // JPA
  }

  public AuditLog(
      String actor,
      String actorRole,
      String action,
      String entityName,
      String entityId,
      String previousState,
      String newState,
      String origin) {
    this.actor = actor;
    this.actorRole = actorRole;
    this.action = action;
    this.entityName = entityName;
    this.entityId = entityId;
    this.previousState = previousState;
    this.newState = newState;
    this.origin = origin;
    this.occurredAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getActor() {
    return actor;
  }

  public String getActorRole() {
    return actorRole;
  }

  public String getAction() {
    return action;
  }

  public String getEntityName() {
    return entityName;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getPreviousState() {
    return previousState;
  }

  public String getNewState() {
    return newState;
  }

  public String getOrigin() {
    return origin;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }
}
