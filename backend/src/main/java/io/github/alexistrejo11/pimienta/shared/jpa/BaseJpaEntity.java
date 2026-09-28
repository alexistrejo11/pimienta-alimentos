package io.github.alexistrejo11.pimienta.shared.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.LocalDateTime;

/**
 * Auditing fields shared by JPA entities. Domain aggregates guarantee non-null
 * timestamps; rows from legacy imports or partial writes may have nulls, so
 * they are filled on persist.
 */
@MappedSuperclass
public abstract class BaseJpaEntity extends VersionedJpaEntity {

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

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

  /** Sets {@code createdAt} and {@code updatedAt} to {@code now} when null (new row). */
  public void fillCreatedAndUpdatedIfNull() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  /** Sets {@code updatedAt} to {@code now} when null (e.g. partial legacy row). */
  public void fillUpdatedIfNull() {
    if (updatedAt == null) {
      updatedAt = LocalDateTime.now();
    }
  }

  @PrePersist
  protected void fillAuditTimestamps() {
    fillCreatedAndUpdatedIfNull();
  }
}
