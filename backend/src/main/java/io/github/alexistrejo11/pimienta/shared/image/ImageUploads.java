package io.github.alexistrejo11.pimienta.shared.image;

import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

/** Maps an HTTP multipart part into {@link ImageUploadPayload} at the inbound adapter. */
public final class ImageUploads {

  private ImageUploads() {}

  public static ImageUploadPayload fromNullable(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      return null;
    }
    return from(file);
  }

  public static List<ImageUploadPayload> listFrom(List<MultipartFile> files) {
    if (files == null || files.isEmpty()) {
      return List.of();
    }
    List<ImageUploadPayload> payloads = new ArrayList<>(files.size());
    for (MultipartFile file : files) {
      payloads.add(from(file));
    }
    return List.copyOf(payloads);
  }

  public static ImageUploadPayload from(MultipartFile file) {
    if (file == null) {
      return new ImageUploadPayload(new byte[0], null, null, 0);
    }
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException ex) {
      throw new BusinessValidationException(
          "No se pudo leer la imagen.",
          Map.of("reason", "UNREADABLE"),
          "multipart read failed");
    }
    long declaredSize = file.getSize() >= 0 ? file.getSize() : bytes.length;
    return new ImageUploadPayload(bytes, file.getContentType(), file.getOriginalFilename(), declaredSize);
  }
}
