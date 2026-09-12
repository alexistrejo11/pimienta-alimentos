package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "storage_locations",
    indexes = {
      @Index(name = "idx_storage_locations_code", columnList = "code"),
      @Index(name = "idx_storage_locations_parent_id", columnList = "parent_id"),
      @Index(name = "idx_storage_locations_deleted_at", columnList = "deleted_at")
    })
public class StorageLocationJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private LocationType type;

  @Column(name = "parent_id")
  private Long parentId;

  @Column(name = "max_capacity", nullable = false)
  private int maxCapacity;

  @Column(name = "occupied_capacity", nullable = false)
  private int occupiedCapacity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private LocationStatus status;

  @Column(name = "headquarter_id")
  private Long headquarterId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(nullable = false)
  private Long version;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocationType getType() {
    return type;
  }

  public void setType(LocationType type) {
    this.type = type;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public int getMaxCapacity() {
    return maxCapacity;
  }

  public void setMaxCapacity(int maxCapacity) {
    this.maxCapacity = maxCapacity;
  }

  public int getOccupiedCapacity() {
    return occupiedCapacity;
  }

  public void setOccupiedCapacity(int occupiedCapacity) {
    this.occupiedCapacity = occupiedCapacity;
  }

  public LocationStatus getStatus() {
    return status;
  }

  public void setStatus(LocationStatus status) {
    this.status = status;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
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
