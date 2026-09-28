package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.repository;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.StorageLocationSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.StorageLocationJpaEntity;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.mapper.StorageLocationPersistenceMapper;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.specification.StorageLocationSpecifications;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class StorageLocationRepositoryImpl implements StorageLocationRepository {

  private final StorageLocationJpaRepository jpaRepository;
  private final InventoryJpaRepository inventoryJpaRepository;

  @PersistenceContext
  private EntityManager entityManager;

  public StorageLocationRepositoryImpl(
      StorageLocationJpaRepository jpaRepository, InventoryJpaRepository inventoryJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.inventoryJpaRepository = inventoryJpaRepository;
  }

  @Override
  public Optional<StorageLocation> findById(long id) {
    return jpaRepository.findByIdAndDeletedAtIsNull(id).map(StorageLocationPersistenceMapper::toDomain);
  }

  @Override
  public Optional<StorageLocation> findByIdForUpdate(long id) {
    StorageLocationJpaEntity entity = entityManager.find(StorageLocationJpaEntity.class, id);
    if (entity == null) {
      return Optional.empty();
    }
    // refresh (not a locking query) so a copy already in the persistence context is reloaded at
    // the committed @Version once the lock is granted, instead of failing as a version conflict.
    entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
    if (entity.getDeletedAt() != null) {
      return Optional.empty();
    }
    return Optional.of(StorageLocationPersistenceMapper.toDomain(entity));
  }

  @Override
  public List<StorageLocation> findAllNonDeleted() {
    return jpaRepository.findByDeletedAtIsNullOrderByCodeAsc().stream()
        .map(StorageLocationPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public List<StorageLocation> findByParentId(Long parentId) {
    return jpaRepository.findByParentIdAndDeletedAtIsNullOrderByCodeAsc(parentId).stream()
        .map(StorageLocationPersistenceMapper::toDomain)
        .toList();
  }

  @Override
  public Optional<StorageLocation> findPosByHeadquarterId(long headquarterId) {
    return jpaRepository
        .findByHeadquarterIdAndTypeAndDeletedAtIsNull(
            headquarterId, StorageLocation.LocationType.POS)
        .map(StorageLocationPersistenceMapper::toDomain);
  }

  @Override
  public Optional<StorageLocation> findByCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return jpaRepository
        .findByCodeAndDeletedAtIsNull(code.trim())
        .map(StorageLocationPersistenceMapper::toDomain);
  }

  @Override
  public Page<StorageLocation> search(StorageLocationSearchCriteria criteria, Pageable pageable) {
    Specification<StorageLocationJpaEntity> spec = StorageLocationSpecifications.fromCriteria(criteria);
    return jpaRepository.findAll(spec, pageable).map(StorageLocationPersistenceMapper::toDomain);
  }

  @Override
  public long countInventoryRowsByLocationId(long locationId) {
    return inventoryJpaRepository.countByLocationIdAndDeletedAtIsNull(locationId);
  }

  @Override
  public StorageLocation save(StorageLocation location) {
    StorageLocationJpaEntity entity = StorageLocationPersistenceMapper.toJpa(location);
    StorageLocationJpaEntity saved = jpaRepository.save(entity);
    return StorageLocationPersistenceMapper.toDomain(saved);
  }
}
