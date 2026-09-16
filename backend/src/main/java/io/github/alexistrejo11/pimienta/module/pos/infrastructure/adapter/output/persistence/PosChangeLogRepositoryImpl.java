package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosChangeLogRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosChangeLogRepository.PosChangeLogEntry;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosChangeLogJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosChangeLogSpringDataRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class PosChangeLogRepositoryImpl implements PosChangeLogRepository {

  private final PosChangeLogSpringDataRepository jpa;

  public PosChangeLogRepositoryImpl(PosChangeLogSpringDataRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public PosChangeLogEntry save(PosChangeLogEntry entry) {
    PosChangeLogJpaEntity entity = new PosChangeLogJpaEntity();
    entity.setHeadquarterId(entry.headquarterId());
    entity.setEntityType(entry.entityType());
    entity.setEntityId(entry.entityId());
    entity.setOperation(entry.operation());
    entity.setProjectionPayload(entry.projectionPayload());
    entity.setCreatedAt(entry.createdAt() != null ? entry.createdAt() : LocalDateTime.now());
    return toEntry(jpa.save(entity));
  }

  @Override
  public Optional<PosChangeLogEntry> findFirstByHeadquarterId(long headquarterId) {
    return jpa.findFirstByHeadquarterIdOrderBySequenceAsc(headquarterId).map(this::toEntry);
  }

  @Override
  public Optional<PosChangeLogEntry> findLastByHeadquarterId(long headquarterId) {
    return jpa.findFirstByHeadquarterIdOrderBySequenceDesc(headquarterId).map(this::toEntry);
  }

  @Override
  public List<PosChangeLogEntry> findAfterSequence(
      long headquarterId, long sequence, long upperBound, int limit) {
    return jpa.findAfterSequence(
            headquarterId, sequence, upperBound, PageRequest.of(0, limit)).stream()
        .map(this::toEntry)
        .toList();
  }

  private PosChangeLogEntry toEntry(PosChangeLogJpaEntity entity) {
    return new PosChangeLogEntry(
        entity.getSequence(),
        entity.getHeadquarterId(),
        entity.getEntityType(),
        entity.getEntityId(),
        entity.getOperation(),
        entity.getProjectionPayload(),
        entity.getCreatedAt());
  }
}
