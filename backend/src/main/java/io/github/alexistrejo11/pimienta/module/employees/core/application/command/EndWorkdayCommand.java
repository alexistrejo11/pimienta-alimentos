package io.github.alexistrejo11.pimienta.module.employees.core.application.command;

import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import java.time.LocalDate;

public record EndWorkdayCommand(long employeeId, LocalDate workDate, ImageUploadPayload checkOutEvidencePhoto) {

  public EndWorkdayCommand {
    if (workDate == null) {
      throw new IllegalArgumentException("workDate must not be null");
    }
  }
}
