package io.github.alexistrejo11.pimienta.shared.storage;

import java.util.Locale;

/** Safe S3 path segments. Object names themselves are UUIDs, not user filenames. */
public final class StorageFileNaming {

  private StorageFileNaming() {}

  /**
   * Lowercase extension without the dot. Falls back to {@code bin} when there is no extension
   * or the suffix is not a short alphanumeric token.
   */
  public static String extension(String originalFilename) {
    if (originalFilename == null || originalFilename.isBlank()) {
      return "bin";
    }
    int dot = originalFilename.lastIndexOf('.');
    if (dot < 0 || dot == originalFilename.length() - 1) {
      return "bin";
    }
    String raw = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    if (!raw.matches("[a-z0-9]{1,8}")) {
      return "bin";
    }
    return raw;
  }

  /**
   * Strips characters that are unsafe in S3 keys and collapses repeated underscores.
   * Returns {@code unknown} for blank input.
   */
  public static String sanitize(String segment) {
    if (segment == null || segment.isBlank()) {
      return "unknown";
    }
    String cleaned =
        segment
            .strip()
            .replaceAll("[^a-zA-Z0-9_-]", "_")
            .replaceAll("_{2,}", "_")
            .replaceAll("^_+|_+$", "");
    return cleaned.isBlank() ? "unknown" : cleaned;
  }
}
