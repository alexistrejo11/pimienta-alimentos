package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pos_operators")
public class PosOperatorJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "display_name", nullable = false)
  private String displayName;

  @Enumerated(EnumType.STRING)
  @Column(name = "pos_role", nullable = false, length = 32)
  private PosRole posRole;

  @Column(name = "pin_hash", nullable = false)
  private String pinHash;

  @Column(nullable = false)
  private boolean active;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(
      name = "pos_operator_headquarter",
      joinColumns = @JoinColumn(name = "operator_id"))
  @Column(name = "headquarter_id")
  private Set<Long> headquarterIds = new HashSet<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public PosRole getPosRole() {
    return posRole;
  }

  public void setPosRole(PosRole posRole) {
    this.posRole = posRole;
  }

  public String getPinHash() {
    return pinHash;
  }

  public void setPinHash(String pinHash) {
    this.pinHash = pinHash;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Set<Long> getHeadquarterIds() {
    return headquarterIds;
  }

  public void setHeadquarterIds(Set<Long> headquarterIds) {
    this.headquarterIds = headquarterIds != null ? headquarterIds : new HashSet<>();
  }
}
