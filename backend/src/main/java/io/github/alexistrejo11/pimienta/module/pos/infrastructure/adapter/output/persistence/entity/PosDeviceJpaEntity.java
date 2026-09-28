package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "pos_devices")
public class PosDeviceJpaEntity extends BaseJpaEntity implements Persistable<UUID> {

  @Id
  private UUID id;

  @Transient
  private boolean newEntity = true;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(name = "visible_code", nullable = false, length = 32)
  private String visibleCode;

  @Column(name = "device_name", nullable = false)
  private String deviceName;

  @Column(name = "app_version", length = 64)
  private String appVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PosDeviceStatus status;

  @Column(name = "min_app_version", nullable = false, length = 64)
  private String minAppVersion;

  @Column(name = "last_device_sequence")
  private Long lastDeviceSequence;

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

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public String getVisibleCode() {
    return visibleCode;
  }

  public void setVisibleCode(String visibleCode) {
    this.visibleCode = visibleCode;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public void setDeviceName(String deviceName) {
    this.deviceName = deviceName;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }

  public PosDeviceStatus getStatus() {
    return status;
  }

  public void setStatus(PosDeviceStatus status) {
    this.status = status;
  }

  public String getMinAppVersion() {
    return minAppVersion;
  }

  public void setMinAppVersion(String minAppVersion) {
    this.minAppVersion = minAppVersion;
  }

  public Long getLastDeviceSequence() {
    return lastDeviceSequence;
  }

  public void setLastDeviceSequence(Long lastDeviceSequence) {
    this.lastDeviceSequence = lastDeviceSequence;
  }
}
