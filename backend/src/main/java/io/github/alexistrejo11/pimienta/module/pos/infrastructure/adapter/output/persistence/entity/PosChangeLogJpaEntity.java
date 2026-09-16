package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pos_change_log")
public class PosChangeLogJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "change_sequence")
  private Long sequence;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(name = "entity_type", nullable = false, length = 32)
  private String entityType;

  @Column(name = "entity_id", nullable = false, length = 128)
  private String entityId;

  @Column(nullable = false, length = 16)
  private String operation;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "projection_payload", nullable = false)
  private String projectionPayload;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public Long getSequence() { return sequence; }
  public void setSequence(Long sequence) { this.sequence = sequence; }
  public Long getHeadquarterId() { return headquarterId; }
  public void setHeadquarterId(Long headquarterId) { this.headquarterId = headquarterId; }
  public String getEntityType() { return entityType; }
  public void setEntityType(String entityType) { this.entityType = entityType; }
  public String getEntityId() { return entityId; }
  public void setEntityId(String entityId) { this.entityId = entityId; }
  public String getOperation() { return operation; }
  public void setOperation(String operation) { this.operation = operation; }
  public String getProjectionPayload() { return projectionPayload; }
  public void setProjectionPayload(String projectionPayload) { this.projectionPayload = projectionPayload; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
