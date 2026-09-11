package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.employees.core.port.output.EmployeeStorageService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Persists S3 object keys (or legacy absolute URLs) on aggregates; exposes HTTPS URLs on API
 * responses. When no photo is stored, synthesizes a ui-avatars placeholder from the person's name
 * — the placeholder is never persisted.
 */
@Component
public class EmployeePhotoUrlPresenter {

  private static final String UI_AVATARS_BASE = "https://ui-avatars.com/api/?name=";

  private final EmployeeStorageService employeeStorageService;

  public EmployeePhotoUrlPresenter(EmployeeStorageService employeeStorageService) {
    this.employeeStorageService = employeeStorageService;
  }

  /** Attendance evidence / raw stored keys or URLs (no avatar fallback). */
  public String present(String stored) {
    if (stored == null || stored.isBlank()) {
      return stored == null ? "" : stored;
    }
    String t = stored.strip();
    if (t.startsWith("http://") || t.startsWith("https://")) {
      return t;
    }
    return employeeStorageService.generatePresignedUrl(t);
  }

  /**
   * Profile photo for list/detail: blank/null → ui-avatars; legacy absolute URL → as-is; S3 key →
   * presigned.
   */
  public String presentProfile(String stored, String firstName, String lastName) {
    if (stored == null || stored.isBlank()) {
      return placeholderUrl(firstName, lastName);
    }
    String t = stored.strip();
    if (t.startsWith("http://") || t.startsWith("https://")) {
      return t;
    }
    return employeeStorageService.generatePresignedUrl(t);
  }

  static String placeholderUrl(String firstName, String lastName) {
    String first = firstName != null ? firstName.strip() : "";
    String last = lastName != null ? lastName.strip() : "";
    String label;
    if (!first.isEmpty() && !last.isEmpty()) {
      label = first.charAt(0) + " " + last.charAt(0);
    } else if (!first.isEmpty()) {
      label = first.length() >= 2 ? first.substring(0, 2) : first;
    } else if (!last.isEmpty()) {
      label = last.length() >= 2 ? last.substring(0, 2) : last;
    } else {
      label = "?";
    }
    return UI_AVATARS_BASE
        + URLEncoder.encode(label, StandardCharsets.UTF_8)
        + "&size=512";
  }
}
