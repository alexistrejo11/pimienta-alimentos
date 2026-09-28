package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.files.core.application.query.FileAssetSearchCriteria;
import io.github.alexistrejo11.pimienta.module.files.core.domain.FileAsset;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileAssetRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class FileAssetRepositoryImpl implements FileAssetRepository {

  private final FileAssetJpaRepository jpaRepository;

  public FileAssetRepositoryImpl(FileAssetJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public FileAsset save(FileAsset fileAsset) {
    FileAssetJpaEntity entity = FileAssetPersistenceMapper.toJpa(fileAsset);
    entity.fillCreatedAndUpdatedIfNull();
    return FileAssetPersistenceMapper.toDomain(jpaRepository.save(entity));
  }

  @Override
  public Optional<FileAsset> findById(UUID id) {
    return jpaRepository.findById(id).map(FileAssetPersistenceMapper::toDomain);
  }

  @Override
  public Optional<FileAsset> findActiveById(UUID id) {
    return jpaRepository
        .findOne(
            Specification.where(
                (root, query, cb) ->
                    cb.and(
                        cb.equal(root.get("id"), id),
                        cb.isNull(root.get("deletedAt")))))
        .map(FileAssetPersistenceMapper::toDomain);
  }

  @Override
  public Page<FileAsset> search(FileAssetSearchCriteria criteria, Pageable pageable) {
    return jpaRepository
        .findAll(FileAssetSpecifications.fromCriteria(criteria), pageable)
        .map(FileAssetPersistenceMapper::toDomain);
  }

  @Override
  public void softDelete(UUID id) {
    jpaRepository
        .findById(id)
        .ifPresent(
            entity -> {
              entity.setDeletedAt(LocalDateTime.now());
              entity.setUpdatedAt(LocalDateTime.now());
              jpaRepository.save(entity);
            });
  }
}
