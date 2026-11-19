package io.github.alexistrejo11.pimienta.module.employees.core.application.command;

import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import java.time.LocalDate;

public record StartWorkdayCommand(
    long employeeId, long headquarterId, LocalDate workDate, ImageUploadPayload checkInEvidencePhoto) {

  public StartWorkdayCommand {
    if (workDate == null) {
      throw new IllegalArgumentException("workDate must not be null");
    }
  }
}
