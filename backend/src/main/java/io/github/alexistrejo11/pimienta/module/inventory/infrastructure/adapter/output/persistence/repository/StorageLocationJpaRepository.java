package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationType;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.StorageLocationJpaEntity;

public interface StorageLocationJpaRepository
    extends JpaRepository<StorageLocationJpaEntity, Long>, JpaSpecificationExecutor<StorageLocationJpaEntity> {

  Optional<StorageLocationJpaEntity> findByIdAndDeletedAtIsNull(Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select l from StorageLocationJpaEntity l where l.id = :id and l.deletedAt is null")
  Optional<StorageLocationJpaEntity> findByIdForUpdate(@Param("id") Long id);

  List<StorageLocationJpaEntity> findByDeletedAtIsNullOrderByCodeAsc();

  List<StorageLocationJpaEntity> findByParentIdAndDeletedAtIsNullOrderByCodeAsc(Long parentId);

  Optional<StorageLocationJpaEntity> findByHeadquarterIdAndTypeAndDeletedAtIsNull(
      Long headquarterId, LocationType type);

  Optional<StorageLocationJpaEntity> findByCodeAndDeletedAtIsNull(String code);
}
