package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.module.files.core.domain.FileAsset;
import java.time.LocalDateTime;

final class FileAssetPersistenceMapper {

  private FileAssetPersistenceMapper() {}

  static FileAssetJpaEntity toJpa(FileAsset domain) {
    FileAssetJpaEntity e = new FileAssetJpaEntity();
    e.setId(domain.getId());
    e.setCategory(domain.getCategory());
    e.setModule(domain.getModule());
    e.setEntityType(domain.getEntityType());
    e.setEntityId(domain.getEntityId());
    e.setS3Key(domain.getS3Key());
    e.setOriginalName(domain.getOriginalName());
    e.setContentType(domain.getContentType());
    e.setFileSizeBytes(domain.getFileSizeBytes());
    e.setDescription(domain.getDescription());
    e.setUploadedByUserId(domain.getUploadedByUserId());
    LocalDateTime now = LocalDateTime.now();
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : now);
    e.setUpdatedAt(now);
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  static FileAsset toDomain(FileAssetJpaEntity e) {
    return FileAsset.builder()
        .id(e.getId())
        .category(e.getCategory())
        .module(e.getModule())
        .entityType(e.getEntityType())
        .entityId(e.getEntityId())
        .s3Key(e.getS3Key())
        .originalName(e.getOriginalName())
        .contentType(e.getContentType())
        .fileSizeBytes(e.getFileSizeBytes())
        .description(e.getDescription())
        .uploadedByUserId(e.getUploadedByUserId())
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .deletedAt(e.getDeletedAt())
        .version(e.getVersion())
        .reconstruct();
  }
}
