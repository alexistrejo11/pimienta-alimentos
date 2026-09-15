package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.Headquarter;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterStatistics;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterJpaEntity;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterJpaRepository;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class HeadquarterRepositoryImpl implements HeadquarterRepository {

  private final HeadquarterJpaRepository jpaRepository;
  private final HeadquarterPersistenceMapper mapper;

  public HeadquarterRepositoryImpl(
      HeadquarterJpaRepository jpaRepository, HeadquarterPersistenceMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Page<Headquarter> findAll(Pageable pageable) {
    Objects.requireNonNull(pageable, "pageable");
    return jpaRepository
        .findByDeletedAtIsNull(pageable)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<Headquarter> findById(Long id) {
    if (id == null || id == 0L) {
      return Optional.empty();
    }

    return jpaRepository.findByIdAndDeletedAtIsNull(id)
        .map(mapper::toDomain);
  }

  @Override
  public Optional<Headquarter> findByName(String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    return jpaRepository
        .findByNameAndDeletedAtIsNull(name)
        .map(mapper::toDomain)
        .filter(Objects::nonNull);
  }

  @Override
  @Transactional
  public Headquarter save(Headquarter headquarter) {
    Objects.requireNonNull(headquarter);

    HeadquarterJpaEntity entity = mapper.toEntity(headquarter);
    HeadquarterJpaEntity saved = jpaRepository.save(entity);

    return mapper.toDomain(saved);

  }

  @Override
  public long countActive() {
    return jpaRepository.countByDeletedAtIsNull();
  }

  @Override
  public HeadquarterStatistics statistics() {
    long total = jpaRepository.count();
    long softDeleted = jpaRepository.countByDeletedAtIsNotNull();
    long active = total - softDeleted;
    return new HeadquarterStatistics(total, active, softDeleted);
  }
}
