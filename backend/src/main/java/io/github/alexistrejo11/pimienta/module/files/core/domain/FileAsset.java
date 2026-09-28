package io.github.alexistrejo11.pimienta.module.files.core.domain;

import io.github.alexistrejo11.pimienta.module.files.core.domain.enums.FileCategory;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate representing a company-managed file stored in S3.
 * Build with {@link #builder()} → {@link SafeBuilder#register()} or
 * {@link SafeBuilder#reconstruct()}.
 */
public class FileAsset extends BaseDomain<UUID> {

  private FileCategory category;
  /** Owning module slug for {@link FileCategory#RESOURCE}, e.g. {@code inventory}, {@code crm}. */
  private String module;
  /** Optional domain entity-type tag within the module, e.g. {@code inventory-item}. */
  private String entityType;
  /** Optional entity id (not FK-enforced). */
  private Long entityId;
  private String s3Key;
  private String originalName;
  private String contentType;
  private Long fileSizeBytes;
  private String description;
  private Long uploadedByUserId;

  private FileAsset() {}

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public FileCategory getCategory() {
    return category;
  }

  public String getModule() {
    return module;
  }

  public String getEntityType() {
    return entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getS3Key() {
    return s3Key;
  }

  public String getOriginalName() {
    return originalName;
  }

  public String getContentType() {
    return contentType;
  }

  public Long getFileSizeBytes() {
    return fileSizeBytes;
  }

  public String getDescription() {
    return description;
  }

  public Long getUploadedByUserId() {
    return uploadedByUserId;
  }

  public static final class SafeBuilder {

    private final FileAsset a = new FileAsset();

    private SafeBuilder() {}

    public SafeBuilder id(UUID id) {
      a.id = id;
      return this;
    }

    public SafeBuilder category(FileCategory category) {
      a.category = category;
      return this;
    }

    public SafeBuilder module(String module) {
      a.module = blank(module);
      return this;
    }

    public SafeBuilder entityType(String entityType) {
      a.entityType = blank(entityType);
      return this;
    }

    public SafeBuilder entityId(Long entityId) {
      a.entityId = entityId;
      return this;
    }

    public SafeBuilder s3Key(String s3Key) {
      a.s3Key = blank(s3Key);
      return this;
    }

    public SafeBuilder originalName(String originalName) {
      a.originalName = blank(originalName);
      return this;
    }

    public SafeBuilder contentType(String contentType) {
      a.contentType = blank(contentType);
      return this;
    }

    public SafeBuilder fileSizeBytes(Long fileSizeBytes) {
      a.fileSizeBytes = fileSizeBytes;
      return this;
    }

    public SafeBuilder description(String description) {
      a.description = blank(description);
      return this;
    }

    public SafeBuilder uploadedByUserId(Long uploadedByUserId) {
      a.uploadedByUserId = uploadedByUserId;
      return this;
    }

    public SafeBuilder createdAt(java.time.LocalDateTime createdAt) {
      a.createdAt = createdAt;
      return this;
    }

    public SafeBuilder updatedAt(java.time.LocalDateTime updatedAt) {
      a.updatedAt = updatedAt;
      return this;
    }

    public SafeBuilder deletedAt(java.time.LocalDateTime deletedAt) {
      a.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder version(Long version) {
      a.version = version;
      return this;
    }

    /** Creation path: validates required invariants. */
    public FileAsset register() {
      Objects.requireNonNull(a.category, "category is required");
      if (a.s3Key == null) {
        throw new IllegalArgumentException("s3Key is required");
      }
      if (a.originalName == null) {
        throw new IllegalArgumentException("originalName is required");
      }
      if (a.contentType == null) {
        throw new IllegalArgumentException("contentType is required");
      }
      if (a.category == io.github.alexistrejo11.pimienta.module.files.core.domain.enums.FileCategory.RESOURCE
          && a.module == null) {
        throw new IllegalArgumentException("module is required for RESOURCE category");
      }
      if (a.id == null) {
        a.id = UUID.randomUUID();
      }
      java.time.LocalDateTime now = java.time.LocalDateTime.now();
      if (a.createdAt == null) {
        a.createdAt = now;
      }
      if (a.updatedAt == null) {
        a.updatedAt = now;
      }
      return a;
    }

    /** Hydration from persistence — no validation. */
    public FileAsset reconstruct() {
      return a;
    }
  }

  private static String blank(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
