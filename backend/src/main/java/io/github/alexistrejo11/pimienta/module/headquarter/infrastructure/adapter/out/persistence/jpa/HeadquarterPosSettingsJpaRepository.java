package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeadquarterPosSettingsJpaRepository
    extends JpaRepository<HeadquarterPosSettingsJpaEntity, Long> {

  Optional<HeadquarterPosSettingsJpaEntity> findByHeadquarterIdAndDeletedAtIsNull(
      Long headquarterId);
}
