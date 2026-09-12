package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;
import java.util.UUID;

public class PosEnrollmentCode extends BaseDomain<Long> {

  private String code;
  private Long headquarterId;
  private LocalDateTime expiresAt;
  private LocalDateTime consumedAt;
  private UUID consumedByDeviceId;

  private PosEnrollmentCode() {
    this.id = 0L;
    this.code = "";
    this.headquarterId = 0L;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public String getCode() {
    return code != null ? code : "";
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public LocalDateTime getConsumedAt() {
    return consumedAt;
  }

  public UUID getConsumedByDeviceId() {
    return consumedByDeviceId;
  }

  public boolean isConsumed() {
    return consumedAt != null;
  }

  public boolean isExpired(LocalDateTime now) {
    return expiresAt != null && now.isAfter(expiresAt);
  }

  public void consume(UUID deviceId, LocalDateTime at) {
    this.consumedAt = at;
    this.consumedByDeviceId = deviceId;
    this.updatedAt = at;
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private Long id;
    private String code;
    private Long headquarterId;
    private LocalDateTime expiresAt;
    private LocalDateTime consumedAt;
    private UUID consumedByDeviceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withCode(String code) {
      this.code = code;
      return this;
    }

    public SafeBuilder withHeadquarterId(Long headquarterId) {
      this.headquarterId = headquarterId;
      return this;
    }

    public SafeBuilder withExpiresAt(LocalDateTime expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    public SafeBuilder withConsumedAt(LocalDateTime consumedAt) {
      this.consumedAt = consumedAt;
      return this;
    }

    public SafeBuilder withConsumedByDeviceId(UUID consumedByDeviceId) {
      this.consumedByDeviceId = consumedByDeviceId;
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

    public PosEnrollmentCode reconstruct() {
      PosEnrollmentCode c = new PosEnrollmentCode();
      c.id = id != null ? id : 0L;
      c.code = code != null ? code : "";
      c.headquarterId = headquarterId != null ? headquarterId : 0L;
      c.expiresAt = expiresAt;
      c.consumedAt = consumedAt;
      c.consumedByDeviceId = consumedByDeviceId;
      c.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      c.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
      c.deletedAt = deletedAt;
      c.version = version != null ? version : 0L;
      return c;
    }

    public PosEnrollmentCode register() {
      PosEnrollmentCode c = reconstruct();
      c.id = 0L;
      c.createdAt = LocalDateTime.now();
      c.updatedAt = c.createdAt;
      c.version = 0L;
      return c;
    }
  }
}
