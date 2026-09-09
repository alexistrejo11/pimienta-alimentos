package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "pos_sync_incidents")
public class PosSyncIncidentJpaEntity implements Persistable<UUID> {

  @Id private UUID id;

  @Transient private boolean newEntity = true;

  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(name = "reason_code", nullable = false, length = 64)
  private String reasonCode;

  @Column(length = 1024)
  private String detail;

  @Column(name = "accepted_at")
  private LocalDateTime acceptedAt;

  @Column(name = "accepted_by")
  private Long acceptedBy;

  @Column(name = "accept_label", length = 128)
  private String acceptLabel;

  @Column(name = "accept_note", length = 1024)
  private String acceptNote;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return newEntity;
  }

  public void setNewEntity(boolean newEntity) {
    this.newEntity = newEntity;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public String getReasonCode() {
    return reasonCode;
  }

  public void setReasonCode(String reasonCode) {
    this.reasonCode = reasonCode;
  }

  public String getDetail() {
    return detail;
  }

  public void setDetail(String detail) {
    this.detail = detail;
  }

  public LocalDateTime getAcceptedAt() {
    return acceptedAt;
  }

  public void setAcceptedAt(LocalDateTime acceptedAt) {
    this.acceptedAt = acceptedAt;
  }

  public Long getAcceptedBy() {
    return acceptedBy;
  }

  public void setAcceptedBy(Long acceptedBy) {
    this.acceptedBy = acceptedBy;
  }

  public String getAcceptLabel() {
    return acceptLabel;
  }

  public void setAcceptLabel(String acceptLabel) {
    this.acceptLabel = acceptLabel;
  }

  public String getAcceptNote() {
    return acceptNote;
  }

  public void setAcceptNote(String acceptNote) {
    this.acceptNote = acceptNote;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
