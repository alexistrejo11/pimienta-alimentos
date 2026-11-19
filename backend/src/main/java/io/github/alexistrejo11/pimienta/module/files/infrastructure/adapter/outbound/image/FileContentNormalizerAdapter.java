package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.outbound.image;

import io.github.alexistrejo11.pimienta.module.files.core.application.NormalizedFile;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileContentNormalizer;
import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import io.github.alexistrejo11.pimienta.shared.infrastructure.image.ImageSanitizer;
import io.github.alexistrejo11.pimienta.shared.storage.S3WebpUploads;
import io.github.alexistrejo11.pimienta.shared.storage.StorageFileNaming;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class FileContentNormalizerAdapter implements FileContentNormalizer {

  private final ImageSanitizer imageSanitizer;

  public FileContentNormalizerAdapter(ImageSanitizer imageSanitizer) {
    this.imageSanitizer = imageSanitizer;
  }

  @Override
  public NormalizedFile normalize(byte[] content, String declaredContentType, String originalFilename) {
    byte[] bytes = content == null ? new byte[0] : content;
    boolean declaredImage = isDeclaredImage(declaredContentType);
    if (ImageSanitizer.supportedRaster(bytes) || declaredImage) {
      String declared = declaredImage ? declaredContentType : null;
      byte[] webp =
          imageSanitizer.sanitize(
              new ImageUploadPayload(bytes, declared, null, bytes.length), ImageSanitizer.FILES);
      return new NormalizedFile(webp, S3WebpUploads.CONTENT_TYPE, "webp");
    }
    String type =
        declaredContentType == null || declaredContentType.isBlank()
            ? "application/octet-stream"
            : declaredContentType;
    return new NormalizedFile(bytes, type, StorageFileNaming.extension(originalFilename));
  }

  private static boolean isDeclaredImage(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return false;
    }
    int separator = contentType.indexOf(';');
    String type = (separator >= 0 ? contentType.substring(0, separator) : contentType).trim().toLowerCase(Locale.ROOT);
    return type.equals("image/jpeg")
        || type.equals("image/jpg")
        || type.equals("image/png")
        || type.equals("image/webp");
  }
}
