package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "pos_sale_categories")
public class PosSaleCategoryJpaEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "headquarter_id", nullable = false) private Long headquarterId;
  @Column(nullable = false, length = 64) private String name;
  @Column(name = "display_order", nullable = false) private int displayOrder;
  @Column(nullable = false) private boolean active;
  @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
  @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
  @Column(name = "deleted_at") private LocalDateTime deletedAt;
  @Version @Column(nullable = false) private Long version;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getHeadquarterId() { return headquarterId; }
  public void setHeadquarterId(Long value) { headquarterId = value; }
  public String getName() { return name; }
  public void setName(String value) { name = value; }
  public int getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(int value) { displayOrder = value; }
  public boolean isActive() { return active; }
  public void setActive(boolean value) { active = value; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime value) { createdAt = value; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
  public LocalDateTime getDeletedAt() { return deletedAt; }
  public void setDeletedAt(LocalDateTime value) { deletedAt = value; }
  public Long getVersion() { return version; }
  public void setVersion(Long value) { version = value; }
}
