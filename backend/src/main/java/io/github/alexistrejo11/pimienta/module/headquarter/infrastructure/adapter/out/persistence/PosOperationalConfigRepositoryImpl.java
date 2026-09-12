package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosOperationalConfigRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterPosSettingsJpaEntity;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa.HeadquarterPosSettingsJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PosOperationalConfigRepositoryImpl implements PosOperationalConfigRepository {

  private final HeadquarterPosSettingsJpaRepository jpaRepository;

  public PosOperationalConfigRepositoryImpl(HeadquarterPosSettingsJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<PosOperationalConfig> findByHeadquarterId(long headquarterId) {
    return jpaRepository
        .findByHeadquarterIdAndDeletedAtIsNull(headquarterId)
        .map(PosOperationalConfigPersistenceMapper::toDomain);
  }

  @Override
  public PosOperationalConfig save(PosOperationalConfig config) {
    HeadquarterPosSettingsJpaEntity saved =
        jpaRepository.save(PosOperationalConfigPersistenceMapper.toEntity(config));
    return PosOperationalConfigPersistenceMapper.toDomain(saved);
  }
}
