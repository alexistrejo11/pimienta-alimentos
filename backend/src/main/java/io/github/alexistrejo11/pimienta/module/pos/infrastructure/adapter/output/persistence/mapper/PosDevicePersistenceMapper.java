package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosDeviceJpaEntity;
import java.time.LocalDateTime;

public final class PosDevicePersistenceMapper {

  private PosDevicePersistenceMapper() {}

  public static PosDevice toDomain(PosDeviceJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return PosDevice.builder()
        .withId(entity.getId())
        .withHeadquarterId(entity.getHeadquarterId())
        .withVisibleCode(entity.getVisibleCode())
        .withDeviceName(entity.getDeviceName())
        .withAppVersion(entity.getAppVersion())
        .withStatus(entity.getStatus())
        .withMinAppVersion(entity.getMinAppVersion())
        .withLastDeviceSequence(entity.getLastDeviceSequence())
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static PosDeviceJpaEntity toEntity(PosDevice domain) {
    PosDeviceJpaEntity e = new PosDeviceJpaEntity();
    e.setId(domain.getId());
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setVisibleCode(
        domain.getVisibleCode() != null && !domain.getVisibleCode().isBlank()
            ? domain.getVisibleCode().strip()
            : "T0");
    e.setDeviceName(
        domain.getDeviceName() != null && !domain.getDeviceName().isBlank()
            ? domain.getDeviceName().strip()
            : "POS device");
    e.setAppVersion(blankToNull(domain.getAppVersion()));
    e.setStatus(domain.getStatus() != null ? domain.getStatus() : PosDeviceStatus.PENDING);
    e.setMinAppVersion(
        domain.getMinAppVersion() != null && !domain.getMinAppVersion().isBlank()
            ? domain.getMinAppVersion()
            : "1.0.0");
    e.setLastDeviceSequence(domain.getLastDeviceSequence());
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
