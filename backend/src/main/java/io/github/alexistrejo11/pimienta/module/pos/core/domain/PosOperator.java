package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PosOperator extends BaseDomain<Long> {

  private Long userId;
  private String displayName;
  private PosRole posRole;
  private String pinHash;
  private boolean active;
  private Set<Long> headquarterIds = new HashSet<>();

  private PosOperator() {
    this.id = 0L;
    this.displayName = "";
    this.posRole = PosRole.CASHIER;
    this.pinHash = "";
    this.active = true;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public Long getUserId() {
    return userId;
  }

  public String getDisplayName() {
    return displayName != null ? displayName : "";
  }

  public PosRole getPosRole() {
    return posRole != null ? posRole : PosRole.CASHIER;
  }

  public String getPinHash() {
    return pinHash != null ? pinHash : "";
  }

  public boolean isActive() {
    return active;
  }

  public Set<Long> getHeadquarterIds() {
    return Collections.unmodifiableSet(headquarterIds);
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName != null ? displayName.strip() : "";
  }

  public void setPosRole(PosRole posRole) {
    this.posRole = posRole != null ? posRole : PosRole.CASHIER;
  }

  public void setPinHash(String pinHash) {
    this.pinHash = pinHash != null ? pinHash : "";
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public void assignHeadquarter(Long headquarterId) {
    if (headquarterId != null) {
      headquarterIds.add(headquarterId);
      touch();
    }
  }

  public void unassignHeadquarter(Long headquarterId) {
    if (headquarterId != null) {
      headquarterIds.remove(headquarterId);
      touch();
    }
  }

  public void replaceHeadquarters(Set<Long> ids) {
    headquarterIds.clear();
    if (ids != null) {
      headquarterIds.addAll(ids);
    }
    touch();
  }

  public void softDelete() {
    this.deletedAt = LocalDateTime.now();
    this.active = false;
    touch();
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private Long id;
    private Long userId;
    private String displayName;
    private PosRole posRole;
    private String pinHash;
    private Boolean active;
    private Set<Long> headquarterIds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withUserId(Long userId) {
      this.userId = userId;
      return this;
    }

    public SafeBuilder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public SafeBuilder withPosRole(PosRole posRole) {
      this.posRole = posRole;
      return this;
    }

    public SafeBuilder withPinHash(String pinHash) {
      this.pinHash = pinHash;
      return this;
    }

    public SafeBuilder withActive(Boolean active) {
      this.active = active;
      return this;
    }

    public SafeBuilder withHeadquarterIds(Set<Long> headquarterIds) {
      this.headquarterIds = headquarterIds;
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder withVersion(Long version) {
      this.version = version;
      return this;
    }

    public PosOperator reconstruct() {
      PosOperator o = new PosOperator();
      o.id = id != null ? id : 0L;
      o.userId = userId;
      o.displayName = displayName != null ? displayName : "";
      o.posRole = posRole != null ? posRole : PosRole.CASHIER;
      o.pinHash = pinHash != null ? pinHash : "";
      o.active = active == null || active;
      o.headquarterIds =
          headquarterIds != null ? new HashSet<>(headquarterIds) : new HashSet<>();
      o.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      o.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
      o.deletedAt = deletedAt;
      o.version = version != null ? version : 0L;
      return o;
    }

    public PosOperator register() {
      PosOperator o = reconstruct();
      o.id = 0L;
      o.createdAt = LocalDateTime.now();
      o.updatedAt = o.createdAt;
      o.version = 0L;
      return o;
    }
  }
}
