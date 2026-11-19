package io.github.alexistrejo11.pimienta.shared.image;

/**
 * Image bytes accepted at the web edge. Ports and use cases take this value, not {@code MultipartFile}.
 * {@code originalFilename} is diagnostic only and must not be copied into object keys or S3 metadata.
 */
public record ImageUploadPayload(
    byte[] content, String declaredContentType, String originalFilename, long size) {

  public ImageUploadPayload {
    content = content == null ? new byte[0] : content.clone();
    if (size < 0) {
      size = content.length;
    }
  }

  public boolean isEmpty() {
    return content.length == 0;
  }
}
