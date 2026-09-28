package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationType;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.StorageLocationJpaEntity;

public interface StorageLocationJpaRepository
    extends JpaRepository<StorageLocationJpaEntity, Long>, JpaSpecificationExecutor<StorageLocationJpaEntity> {

  Optional<StorageLocationJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  List<StorageLocationJpaEntity> findByDeletedAtIsNullOrderByCodeAsc();

  List<StorageLocationJpaEntity> findByParentIdAndDeletedAtIsNullOrderByCodeAsc(Long parentId);

  Optional<StorageLocationJpaEntity> findByHeadquarterIdAndTypeAndDeletedAtIsNull(
      Long headquarterId, LocationType type);

  Optional<StorageLocationJpaEntity> findByCodeAndDeletedAtIsNull(String code);
}
