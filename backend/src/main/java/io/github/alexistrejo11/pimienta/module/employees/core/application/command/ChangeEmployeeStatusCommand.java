package io.github.alexistrejo11.pimienta.module.employees.core.application.command;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;

public record ChangeEmployeeStatusCommand(long employeeId, EmployeeStatus status) {

  public ChangeEmployeeStatusCommand {
    if (employeeId <= 0) {
      throw new IllegalArgumentException("employeeId must be positive");
    }
    if (status == null) {
      throw new IllegalArgumentException("status must not be null");
    }
  }
}
