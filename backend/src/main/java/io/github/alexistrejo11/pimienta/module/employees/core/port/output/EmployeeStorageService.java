package io.github.alexistrejo11.pimienta.module.employees.core.port.output;

import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;

public interface EmployeeStorageService {
  /** Persists under {@code …/employee-photos/{uuid}.webp}. */
  String uploadEmployeePhoto(ImageUploadPayload file);

  /** Persists under {@code …/attendance-evidence/{employeeId}/{kind}/{uuid}.webp}. */
  String uploadAttendanceEvidencePhoto(ImageUploadPayload file, long employeeId, String evidenceKind);

  void delete(String fileUrlOrKey);

  /** Deletes an object. Failures are logged and do not propagate. */
  void deleteQuietly(String fileUrlOrKey);

  String generatePresignedUrl(String key);
}
