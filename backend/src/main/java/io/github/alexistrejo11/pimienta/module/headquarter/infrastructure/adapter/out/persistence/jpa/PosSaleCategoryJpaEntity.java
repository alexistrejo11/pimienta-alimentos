package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pos_sale_categories")
public class PosSaleCategoryJpaEntity extends BaseJpaEntity {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "headquarter_id", nullable = false) private Long headquarterId;
  @Column(nullable = false, length = 64) private String name;
  @Column(nullable = false) private boolean active;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getHeadquarterId() { return headquarterId; }
  public void setHeadquarterId(Long value) { headquarterId = value; }
  public String getName() { return name; }
  public void setName(String value) { name = value; }
  public boolean isActive() { return active; }
  public void setActive(boolean value) { active = value; }
}
