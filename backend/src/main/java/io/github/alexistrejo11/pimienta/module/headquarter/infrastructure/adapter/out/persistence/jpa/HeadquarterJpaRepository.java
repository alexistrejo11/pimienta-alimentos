package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.out.persistence.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HeadquarterJpaRepository extends JpaRepository<HeadquarterJpaEntity, Long> {

  Optional<HeadquarterJpaEntity> findByNameAndDeletedAtIsNull(String name);

  Page<HeadquarterJpaEntity> findByDeletedAtIsNull(Pageable pageable);

  Optional<HeadquarterJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  long countByDeletedAtIsNull();

  long countByDeletedAtIsNotNull();
}
