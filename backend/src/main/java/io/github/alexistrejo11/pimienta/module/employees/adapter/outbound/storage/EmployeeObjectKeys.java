package io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.storage;

import java.util.UUID;

/** UUID WebP keys. The original filename is never part of the key. */
public final class EmployeeObjectKeys {

  private EmployeeObjectKeys() {}

  public static String photo(String folder) {
    return prefix(folder) + "employee-photos/" + UUID.randomUUID() + ".webp";
  }

  public static String attendance(String folder, long employeeId, String evidenceKind) {
    return prefix(folder)
        + "attendance-evidence/"
        + employeeId
        + "/"
        + kind(evidenceKind)
        + "/"
        + UUID.randomUUID()
        + ".webp";
  }

  private static String kind(String evidenceKind) {
    if (evidenceKind == null || evidenceKind.isBlank()) {
      return "evidence";
    }
    String cleaned = evidenceKind.strip().toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("-{2,}", "-");
    if (cleaned.isBlank() || cleaned.equals("-")) {
      return "evidence";
    }
    return cleaned;
  }

  private static String prefix(String folder) {
    if (folder == null || folder.isBlank()) {
      return "";
    }
    String trimmed = folder.strip().replaceAll("/+$", "");
    return trimmed.isEmpty() ? "" : trimmed + "/";
  }
}
