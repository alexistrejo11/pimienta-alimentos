package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper.PosDevicePersistenceMapper;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosDeviceSpringDataRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class PosDeviceRepositoryImpl implements PosDeviceRepository {

  private final PosDeviceSpringDataRepository jpa;

  public PosDeviceRepositoryImpl(PosDeviceSpringDataRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<PosDevice> findById(UUID id) {
    return jpa.findByIdAndDeletedAtIsNull(id).map(PosDevicePersistenceMapper::toDomain);
  }

  @Override
  public Page<PosDevice> findAll(Pageable pageable) {
    return jpa.findByDeletedAtIsNull(pageable).map(PosDevicePersistenceMapper::toDomain);
  }

  @Override
  public Page<PosDevice> findByHeadquarterId(long headquarterId, Pageable pageable) {
    return jpa.findByHeadquarterIdAndDeletedAtIsNull(headquarterId, pageable)
        .map(PosDevicePersistenceMapper::toDomain);
  }

  @Override
  public long countByHeadquarterIdAndStatusNot(long headquarterId, PosDeviceStatus status) {
    return jpa.countByHeadquarterIdAndStatusNotAndDeletedAtIsNull(headquarterId, status);
  }

  @Override
  public long countByHeadquarterId(long headquarterId) {
    return jpa.countByHeadquarterIdAndDeletedAtIsNull(headquarterId);
  }

  @Override
  public PosDevice save(PosDevice device) {
    boolean exists = jpa.existsById(device.getId());
    var entity = PosDevicePersistenceMapper.toEntity(device);
    entity.setNewEntity(!exists);
    return PosDevicePersistenceMapper.toDomain(jpa.save(entity));
  }
}
