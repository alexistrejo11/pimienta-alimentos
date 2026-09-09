package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncTombstone;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncTombstoneRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper.PosSyncTombstonePersistenceMapper;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosSyncTombstoneSpringDataRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PosSyncTombstoneRepositoryImpl implements PosSyncTombstoneRepository {

  private final PosSyncTombstoneSpringDataRepository jpa;

  public PosSyncTombstoneRepositoryImpl(PosSyncTombstoneSpringDataRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public PosSyncTombstone save(PosSyncTombstone tombstone) {
    return PosSyncTombstonePersistenceMapper.toDomain(
        jpa.save(PosSyncTombstonePersistenceMapper.toEntity(tombstone)));
  }

  @Override
  public List<PosSyncTombstone> findByHeadquarterIdAndCreatedAtAfter(
      long headquarterId, LocalDateTime since) {
    return jpa.findByHeadquarterIdAndCreatedAtAfterOrderByCreatedAtAsc(headquarterId, since)
        .stream()
        .map(PosSyncTombstonePersistenceMapper::toDomain)
        .toList();
  }
}
