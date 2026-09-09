package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosOperatorRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper.PosOperatorPersistenceMapper;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosOperatorSpringDataRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class PosOperatorRepositoryImpl implements PosOperatorRepository {

  private final PosOperatorSpringDataRepository jpa;

  public PosOperatorRepositoryImpl(PosOperatorSpringDataRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<PosOperator> findById(long id) {
    return jpa.findByIdAndDeletedAtIsNull(id).map(PosOperatorPersistenceMapper::toDomain);
  }

  @Override
  public Page<PosOperator> findAll(Pageable pageable) {
    return jpa.findByDeletedAtIsNull(pageable).map(PosOperatorPersistenceMapper::toDomain);
  }

  @Override
  public List<PosOperator> findByHeadquarterId(long headquarterId) {
    return jpa.findByHeadquarterIdAndDeletedAtIsNull(headquarterId).stream()
        .map(PosOperatorPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<PosOperator> findDeletedByHeadquarterIdUpdatedAfter(
      long headquarterId, LocalDateTime since) {
    return jpa.findDeletedByHeadquarterIdAndUpdatedAtAfter(headquarterId, since).stream()
        .map(PosOperatorPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public PosOperator save(PosOperator operator) {
    return PosOperatorPersistenceMapper.toDomain(
        jpa.save(PosOperatorPersistenceMapper.toEntity(operator)));
  }
}
