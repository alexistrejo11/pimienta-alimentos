package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pos_enrollment_codes")
public class PosEnrollmentCodeJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  @Column(name = "consumed_at")
  private LocalDateTime consumedAt;

  @Column(name = "consumed_by_device_id")
  private UUID consumedByDeviceId;

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

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public LocalDateTime getConsumedAt() {
    return consumedAt;
  }

  public void setConsumedAt(LocalDateTime consumedAt) {
    this.consumedAt = consumedAt;
  }

  public UUID getConsumedByDeviceId() {
    return consumedByDeviceId;
  }

  public void setConsumedByDeviceId(UUID consumedByDeviceId) {
    this.consumedByDeviceId = consumedByDeviceId;
  }
}
