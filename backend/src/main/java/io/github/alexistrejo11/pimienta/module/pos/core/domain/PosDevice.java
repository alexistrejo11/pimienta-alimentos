package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;
import java.util.UUID;

public class PosDevice extends BaseDomain<UUID> {

  private Long headquarterId;
  private String visibleCode;
  private String deviceName;
  private String appVersion;
  private PosDeviceStatus status;
  private String minAppVersion;
  private Long lastDeviceSequence;

  private PosDevice() {
    this.headquarterId = 0L;
    this.visibleCode = "";
    this.deviceName = "";
    this.status = PosDeviceStatus.PENDING;
    this.minAppVersion = "1.0.0";
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public String getVisibleCode() {
    return visibleCode != null ? visibleCode : "";
  }

  public String getDeviceName() {
    return deviceName != null ? deviceName : "";
  }

  public String getAppVersion() {
    return appVersion;
  }

  public PosDeviceStatus getStatus() {
    return status != null ? status : PosDeviceStatus.PENDING;
  }

  public String getMinAppVersion() {
    return minAppVersion != null ? minAppVersion : "1.0.0";
  }

  public Long getLastDeviceSequence() {
    return lastDeviceSequence;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName != null ? deviceName.strip() : "";
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }

  public void setVisibleCode(String visibleCode) {
    this.visibleCode = visibleCode != null ? visibleCode.strip() : "";
  }

  public void authorize() {
    this.status = PosDeviceStatus.AUTHORIZED;
    touch();
  }

  public void revoke() {
    this.status = PosDeviceStatus.REVOKED;
    touch();
  }

  public boolean isRevoked() {
    return getStatus() == PosDeviceStatus.REVOKED;
  }

  public boolean isAuthorized() {
    return getStatus() == PosDeviceStatus.AUTHORIZED;
  }

  public void recordDeviceSequence(long sequence) {
    if (lastDeviceSequence == null || sequence > lastDeviceSequence) {
      this.lastDeviceSequence = sequence;
      touch();
    }
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private UUID id;
    private Long headquarterId;
    private String visibleCode;
    private String deviceName;
    private String appVersion;
    private PosDeviceStatus status;
    private String minAppVersion;
    private Long lastDeviceSequence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(UUID id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withHeadquarterId(Long headquarterId) {
      this.headquarterId = headquarterId;
      return this;
    }

    public SafeBuilder withVisibleCode(String visibleCode) {
      this.visibleCode = visibleCode;
      return this;
    }

    public SafeBuilder withDeviceName(String deviceName) {
      this.deviceName = deviceName;
      return this;
    }

    public SafeBuilder withAppVersion(String appVersion) {
      this.appVersion = appVersion;
      return this;
    }

    public SafeBuilder withStatus(PosDeviceStatus status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withMinAppVersion(String minAppVersion) {
      this.minAppVersion = minAppVersion;
      return this;
    }

    public SafeBuilder withLastDeviceSequence(Long lastDeviceSequence) {
      this.lastDeviceSequence = lastDeviceSequence;
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

    public PosDevice reconstruct() {
      PosDevice d = new PosDevice();
      d.id = id;
      d.headquarterId = headquarterId != null ? headquarterId : 0L;
      d.visibleCode = visibleCode != null ? visibleCode : "";
      d.deviceName = deviceName != null ? deviceName : "";
      d.appVersion = appVersion;
      d.status = status != null ? status : PosDeviceStatus.PENDING;
      d.minAppVersion = minAppVersion != null ? minAppVersion : "1.0.0";
      d.lastDeviceSequence = lastDeviceSequence;
      d.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      d.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
      d.deletedAt = deletedAt;
      d.version = version != null ? version : 0L;
      return d;
    }

    public PosDevice register() {
      PosDevice d = reconstruct();
      if (d.id == null) {
        d.id = UUID.randomUUID();
      }
      if (d.status == null) {
        d.status = PosDeviceStatus.AUTHORIZED;
      }
      d.createdAt = LocalDateTime.now();
      d.updatedAt = d.createdAt;
      d.version = null;
      return d;
    }
  }
}
