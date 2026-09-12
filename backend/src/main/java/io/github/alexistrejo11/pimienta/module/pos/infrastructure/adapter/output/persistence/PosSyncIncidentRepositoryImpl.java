package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncIncident;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncIncidentRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper.PosSyncIncidentPersistenceMapper;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosSyncIncidentSpringDataRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class PosSyncIncidentRepositoryImpl implements PosSyncIncidentRepository {

  private final PosSyncIncidentSpringDataRepository jpa;

  public PosSyncIncidentRepositoryImpl(PosSyncIncidentSpringDataRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public PosSyncIncident save(PosSyncIncident incident) {
    boolean exists = incident.getId() != null && jpa.existsById(incident.getId());
    var entity = PosSyncIncidentPersistenceMapper.toEntity(incident);
    entity.setNewEntity(!exists);
    return PosSyncIncidentPersistenceMapper.toDomain(jpa.save(entity));
  }

  @Override
  public Optional<PosSyncIncident> findById(UUID id) {
    return jpa.findByIdAndDeletedAtIsNull(id).map(PosSyncIncidentPersistenceMapper::toDomain);
  }

  @Override
  public Page<PosSyncIncident> findFiltered(
      Long headquarterId, Boolean openOnly, Pageable pageable) {
    return jpa.findFiltered(headquarterId, openOnly, pageable)
        .map(PosSyncIncidentPersistenceMapper::toDomain);
  }

  @Override
  public long countOpenByHeadquarterId(long headquarterId) {
    return jpa.countByHeadquarterIdAndAcceptedAtIsNullAndDeletedAtIsNull(headquarterId);
  }
}
