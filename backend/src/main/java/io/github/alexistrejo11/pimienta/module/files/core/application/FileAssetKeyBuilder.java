package io.github.alexistrejo11.pimienta.module.files.core.application;

import io.github.alexistrejo11.pimienta.module.files.core.domain.enums.FileCategory;
import io.github.alexistrejo11.pimienta.shared.storage.StorageFileNaming;
import java.util.UUID;

/**
 * S3 key for a new file asset. The object name is a UUID; the original filename is not included.
 *
 * <pre>
 * {sourcesRoot}/templates/{uuid}.{ext}
 * {sourcesRoot}/company/{uuid}.{ext}
 * {sourcesRoot}/extras/{uuid}.{ext}
 * {sourcesRoot}/resources/{module}/{entityId}/{uuid}.{ext}
 * {sourcesRoot}/resources/{module}/{uuid}.{ext}
 * </pre>
 */
final class FileAssetKeyBuilder {

  private FileAssetKeyBuilder() {}

  static String buildKey(
      String sourcesRoot, FileCategory category, String module, Long entityId, String extension) {
    String root = sourcesRoot == null ? "pimienta/sources" : sourcesRoot.strip().replaceAll("/+$", "");
    String filename = UUID.randomUUID() + "." + StorageFileNaming.extension("file." + extension);
    return switch (category) {
      case TEMPLATE, COMPANY, EXTRAS -> root + "/" + category.getPathSegment() + "/" + filename;
      case RESOURCE -> {
        String modSlug = StorageFileNaming.sanitize(module);
        if (entityId != null) {
          yield root + "/resources/" + modSlug + "/" + entityId + "/" + filename;
        }
        yield root + "/resources/" + modSlug + "/" + filename;
      }
    };
  }
}
