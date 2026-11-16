package io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ContractType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.WorkShift;
import io.github.alexistrejo11.pimienta.shared.util.Strings;

import java.time.LocalDate;

/**
 * Datos laborales y ciclo de vida del vínculo laboral.
 */
public record Employment(
    String position,
    String department,
    ContractType contractType,
    WorkShift workShift,
    LocalDate hireDate,
    LocalDate terminationDate,
    EmployeeStatus status) {

  public Employment {
    position = Strings.nullToEmpty(position);
    department = Strings.nullToEmpty(department);
    contractType = contractType != null ? contractType : ContractType.UNDEFINED;
    workShift = workShift != null ? workShift : WorkShift.UNDEFINED;
    status = status != null ? status : EmployeeStatus.UNDEFINED;
  }

  public static Employment empty() {
    return new Employment(
        "",
        "",
        ContractType.UNDEFINED,
        WorkShift.UNDEFINED,
        null,
        null,
        EmployeeStatus.UNDEFINED);
  }

  public Employment terminate(EmployeeStatus status, LocalDate terminationDate) {
    return new Employment(
        this.position,
        this.department,
        this.contractType,
        this.workShift,
        this.hireDate,
        terminationDate,
        status);
  }

  public Employment setStatus(EmployeeStatus status) {
    return new Employment(
        this.position,
        this.department,
        this.contractType,
        this.workShift,
        this.hireDate,
        this.terminationDate,
        status);
  }

  public Employment setPosition(String position) {
    return new Employment(position, this.department, this.contractType, this.workShift, this.hireDate,
        this.terminationDate, this.status);
  }

  public Employment setDepartment(String department) {
    return new Employment(this.position, department, this.contractType, this.workShift, this.hireDate,
        this.terminationDate, this.status);
  }

  public Employment setContractType(ContractType contractType) {
    return new Employment(this.position, this.department, contractType, this.workShift, this.hireDate,
        this.terminationDate, this.status);
  }

  public Employment setWorkShift(WorkShift workShift) {
    return new Employment(this.position, this.department, this.contractType, workShift, this.hireDate,
        this.terminationDate, this.status);
  }

  public Employment setHireDate(LocalDate hireDate) {
    return new Employment(this.position, this.department, this.contractType, this.workShift, hireDate,
        this.terminationDate, this.status);
  }

  public Employment setTerminationDate(LocalDate terminationDate) {
    return new Employment(this.position, this.department, this.contractType, this.workShift, this.hireDate,
        terminationDate, this.status);
  }

}
