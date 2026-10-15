package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.outbound.image;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptImages;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OperatingExpenseReceiptImageProcessor implements OperatingExpenseReceiptImages {

  public static final int MAX_EDGE = 1920;
  public static final float JPEG_QUALITY = 0.82f;
  public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
  public static final long MAX_TOTAL_BYTES = 6L * 1024 * 1024;

  static {
    ImageIO.scanForPlugins();
  }

  @Override
  public void validateAll(List<MultipartFile> files, int retainedCount) {
    List<MultipartFile> present = files == null ? List.of() : files;
    if (retainedCount < 0 || retainedCount + present.size() > OperatingExpense.MAX_ATTACHMENTS) {
      throw invalid(
          "Un gasto operativo admite como máximo 5 imágenes.",
          "TOO_MANY_ATTACHMENTS",
          "attachment count " + (retainedCount + present.size()));
    }
    long total = 0;
    for (MultipartFile file : present) {
      if (file == null || file.isEmpty() || file.getSize() == 0) {
        throw invalid("La imagen está vacía.", "EMPTY_FILE", "empty attachment");
      }
      if (file.getSize() > MAX_FILE_BYTES) {
        throw invalid(
            "Cada imagen debe pesar como máximo 5 MB.",
            "FILE_TOO_LARGE",
            "attachment bytes " + file.getSize());
      }
      total += file.getSize();
    }
    if (total > MAX_TOTAL_BYTES) {
      throw invalid(
          "Las imágenes juntas no pueden pasar de 6 MB.",
          "TOTAL_TOO_LARGE",
          "attachment bytes " + total);
    }
    for (MultipartFile file : present) {
      validateOne(file);
    }
  }

  @Override
  public byte[] process(MultipartFile file) {
    byte[] raw = validateOne(file);
    try {
      BufferedImage decoded =
          Thumbnails.of(new ByteArrayInputStream(raw))
              .useExifOrientation(true)
              .scale(1)
              .asBufferedImage();
      if (decoded == null) {
        throw unreadable();
      }
      BufferedImage fitted = decoded;
      if (Math.max(decoded.getWidth(), decoded.getHeight()) > MAX_EDGE) {
        fitted =
            Thumbnails.of(decoded)
                .size(MAX_EDGE, MAX_EDGE)
                .keepAspectRatio(true)
                .asBufferedImage();
      }
      BufferedImage rgb =
          new BufferedImage(fitted.getWidth(), fitted.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = rgb.createGraphics();
      try {
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        graphics.drawImage(fitted, 0, 0, null);
      } finally {
        graphics.dispose();
      }
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      Thumbnails.of(rgb).scale(1).outputFormat("jpg").outputQuality(JPEG_QUALITY).toOutputStream(out);
      return out.toByteArray();
    } catch (BusinessValidationException ex) {
      throw ex;
    } catch (IOException | RuntimeException ex) {
      throw unreadable();
    }
  }

  private byte[] validateOne(MultipartFile file) {
    if (file == null || file.isEmpty() || file.getSize() == 0) {
      throw invalid("La imagen está vacía.", "EMPTY_FILE", "empty attachment");
    }
    if (file.getSize() > MAX_FILE_BYTES) {
      throw invalid(
          "Cada imagen debe pesar como máximo 5 MB.",
          "FILE_TOO_LARGE",
          "attachment bytes " + file.getSize());
    }
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException ex) {
      throw unreadable();
    }
    if (bytes.length == 0) {
      throw invalid("La imagen está vacía.", "EMPTY_FILE", "empty attachment bytes");
    }
    if (bytes.length > MAX_FILE_BYTES) {
      throw invalid(
          "Cada imagen debe pesar como máximo 5 MB.",
          "FILE_TOO_LARGE",
          "attachment bytes " + bytes.length);
    }
    String type = mediaType(file.getContentType());
    boolean matches =
        switch (type) {
          case "image/jpeg", "image/jpg" -> isJpeg(bytes);
          case "image/png" -> isPng(bytes);
          case "image/webp" -> isWebp(bytes);
          default -> false;
        };
    if (!matches) {
      throw invalid(
          "Solo se aceptan imágenes JPEG, PNG o WebP.",
          "UNSUPPORTED_TYPE",
          "contentType=" + type);
    }
    return bytes;
  }

  private static String mediaType(String contentType) {
    if (contentType == null || contentType.isBlank()) {
      return "";
    }
    int separator = contentType.indexOf(';');
    String type = separator >= 0 ? contentType.substring(0, separator) : contentType;
    return type.trim().toLowerCase();
  }

  private static boolean isJpeg(byte[] bytes) {
    return bytes.length >= 3
        && (bytes[0] & 0xFF) == 0xFF
        && (bytes[1] & 0xFF) == 0xD8
        && (bytes[2] & 0xFF) == 0xFF;
  }

  private static boolean isPng(byte[] bytes) {
    return bytes.length >= 8
        && (bytes[0] & 0xFF) == 0x89
        && bytes[1] == 0x50
        && bytes[2] == 0x4E
        && bytes[3] == 0x47;
  }

  private static boolean isWebp(byte[] bytes) {
    return bytes.length >= 12
        && bytes[0] == 'R'
        && bytes[1] == 'I'
        && bytes[2] == 'F'
        && bytes[3] == 'F'
        && bytes[8] == 'W'
        && bytes[9] == 'E'
        && bytes[10] == 'B'
        && bytes[11] == 'P';
  }

  private static BusinessValidationException invalid(String message, String reason, String log) {
    return new BusinessValidationException(message, Map.of("reason", reason), log);
  }

  private static BusinessValidationException unreadable() {
    return invalid("No se pudo leer la imagen.", "UNREADABLE", "image decode failed");
  }
}
