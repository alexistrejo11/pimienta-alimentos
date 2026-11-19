package io.github.alexistrejo11.pimienta.module.files.core.port.output;

public interface FileStoragePort {

  /** Stores bytes at {@code s3Key}. The key must not contain the caller's original filename. */
  void store(byte[] content, String contentType, String s3Key);

  /** Hard-deletes the object. Silently no-ops if the key does not exist. */
  void delete(String s3Key);

  /** Deletes an object. Failures are logged and do not propagate. */
  void deleteQuietly(String s3Key);

  /** Returns a 24-hour pre-signed download URL. */
  String presignedDownloadUrl(String s3Key);
}
