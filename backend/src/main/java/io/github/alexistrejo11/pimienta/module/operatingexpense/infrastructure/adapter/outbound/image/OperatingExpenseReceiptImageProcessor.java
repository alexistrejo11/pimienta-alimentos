package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.image;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptImages;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import io.github.alexistrejo11.pimienta.shared.infrastructure.image.ImageSanitizer;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OperatingExpenseReceiptImageProcessor implements OperatingExpenseReceiptImages {

  public static final int MAX_EDGE = ImageSanitizer.RECEIPTS.maxEdgePx();
  public static final long MAX_FILE_BYTES = ImageSanitizer.RECEIPTS.maxFileBytes();
  public static final long MAX_TOTAL_BYTES = 6L * 1024 * 1024;

  private final ImageSanitizer sanitizer;

  public OperatingExpenseReceiptImageProcessor(ImageSanitizer sanitizer) {
    this.sanitizer = sanitizer;
  }

  @Override
  public void validateAll(List<ImageUploadPayload> files, int retainedCount) {
    List<ImageUploadPayload> present = files == null ? List.of() : files;
    if (retainedCount < 0 || retainedCount + present.size() > OperatingExpense.MAX_ATTACHMENTS) {
      throw invalid(
          "Un gasto operativo admite como máximo 5 imágenes.",
          "TOO_MANY_ATTACHMENTS",
          "attachment count " + (retainedCount + present.size()));
    }
    long total = 0;
    for (ImageUploadPayload file : present) {
      if (file == null || file.isEmpty()) {
        throw invalid("La imagen está vacía.", "EMPTY_FILE", "empty attachment");
      }
      long size = Math.max(file.size(), file.content().length);
      if (size > MAX_FILE_BYTES) {
        throw invalid(
            "Cada imagen debe pesar como máximo 5 MB.",
            "FILE_TOO_LARGE",
            "attachment bytes " + size);
      }
      total += size;
    }
    if (total > MAX_TOTAL_BYTES) {
      throw invalid(
          "Las imágenes juntas no pueden pasar de 6 MB.",
          "TOTAL_TOO_LARGE",
          "attachment bytes " + total);
    }
    for (ImageUploadPayload file : present) {
      sanitizer.validate(file, ImageSanitizer.RECEIPTS);
    }
  }

  @Override
  public byte[] process(ImageUploadPayload file) {
    if (file == null || file.isEmpty()) {
      throw invalid("La imagen está vacía.", "EMPTY_FILE", "empty attachment");
    }
    long size = Math.max(file.size(), file.content().length);
    if (size > MAX_FILE_BYTES) {
      throw invalid(
          "Cada imagen debe pesar como máximo 5 MB.", "FILE_TOO_LARGE", "attachment bytes " + size);
    }
    return sanitizer.sanitize(file, ImageSanitizer.RECEIPTS);
  }

  private static BusinessValidationException invalid(String message, String reason, String log) {
    return new BusinessValidationException(message, Map.of("reason", reason), log);
  }
}
