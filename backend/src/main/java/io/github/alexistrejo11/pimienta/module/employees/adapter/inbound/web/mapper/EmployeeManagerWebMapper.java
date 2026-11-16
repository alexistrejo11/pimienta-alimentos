package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.RegisterEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.UpdateEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeStatistics;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeSummary;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.EmployeePhotoUrlPresenter;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.DepartmentHeadcountResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeListItemResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeStatisticsResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response.EmployeeSummaryResponse;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request.RegisterEmployeeRequest;
import io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.request.UpdateEmployeeRequest;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import io.github.alexistrejo11.pimienta.shared.util.Strings;

public final class EmployeeManagerWebMapper {

  private EmployeeManagerWebMapper() {}

  public static EmployeeListItemResponse toListItem(
      Employee employee, EmployeePhotoUrlPresenter photoPresenter) {
    String rawPhoto = employee.getPersonal().photoUrl();
    return new EmployeeListItemResponse(
        employee.getId(),
        employee.getPersonal().firstName() + " " + employee.getPersonal().lastName(),
        photoPresenter.presentProfile(
            rawPhoto, employee.getPersonal().firstName(), employee.getPersonal().lastName()),
        employee.getPersonal().email(),
        employee.getEmployment().department(),
        employee.getEmployment().position(),
        employee.getStatus(),
        employee.getEmployment().hireDate());
  }

  public static EmployeeResponse toResponse(
      Employee employee, EmployeePhotoUrlPresenter photoPresenter) {
    String rawPhoto = employee.getPersonal().photoUrl();
    return new EmployeeResponse(
        employee.getId(),
        employee.getPersonal().firstName(),
        employee.getPersonal().lastName(),
        photoPresenter.presentProfile(
            rawPhoto, employee.getPersonal().firstName(), employee.getPersonal().lastName()),
        employee.getPersonal().email(),
        employee.getPersonal().phone(),
        employee.getPersonal().address(),
        employee.getPersonal().birthDate(),
        employee.getPersonal().nationality(),
        employee.getOfficialIds().curp(),
        employee.getOfficialIds().rfc(),
        employee.getOfficialIds().nss(),
        employee.getOfficialIds().clabe(),
        employee.getOfficialIds().employeeNumber(),
        employee.getEmployment().position(),
        employee.getEmployment().department(),
        employee.getEmployment().contractType(),
        employee.getEmployment().workShift(),
        employee.getEmployment().hireDate(),
        employee.getEmployment().terminationDate(),
        employee.getStatus(),
        employee.getCompensation().salaryPerWeek(),
        employee.getCompensation().bonuses(),
        employee.getCompensation().foodVouchers(),
        employee.getBenefits().integrationFactor(),
        employee.getBenefits().imssWorkerType(),
        employee.getBenefits().imssSalaryType(),
        employee.getBenefits().christmasBonusDays(),
        employee.getBenefits().vacationDays(),
        employee.getBenefits().vacationPremiumPercent(),
        employee.getCreatedAt(),
        employee.getUpdatedAt());
  }

  public static EmployeeStatisticsResponse toStatisticsResponse(EmployeeStatistics statistics) {
    return new EmployeeStatisticsResponse(
        statistics.total(), statistics.active(), statistics.notActive());
  }

  public static EmployeeSummaryResponse toSummaryResponse(EmployeeSummary summary) {
    List<DepartmentHeadcountResponse> rows =
        summary.headcountByDepartment().stream()
            .map(
                row ->
                    new DepartmentHeadcountResponse(row.department(), row.headcount()))
            .toList();
    return new EmployeeSummaryResponse(summary.totalNotDeleted(), rows);
  }

  public static RegisterEmployeeParams toRegisterParams(
      RegisterEmployeeRequest request, MultipartFile photo) {
    String email = Strings.blankToNull(request.email());
    return new RegisterEmployeeParams(
        request.firstName().trim(),
        request.lastName().trim(),
        email != null ? email.toLowerCase() : null,
        Strings.blankToNull(request.phone()),
        Strings.blankToNull(request.address()),
        Strings.blankToNull(request.curp()),
        Strings.blankToNull(request.rfc()),
        Strings.blankToNull(request.nss()),
        Strings.blankToNull(request.clabe()),
        Strings.blankToNull(request.employeeNumber()),
        Strings.blankToNull(request.position()),
        Strings.blankToNull(request.department()),
        request.contractType(),
        request.workShift(),
        request.salaryPerWeek(),
        request.birthDate(),
        request.onboardingPhase(),
        photo);
  }

  public static UpdateEmployeeParams toUpdateParams(
      Long id, UpdateEmployeeRequest request, MultipartFile photo) {
    String email = Strings.blankToNull(request.email());
    return new UpdateEmployeeParams(
        id,
        request.firstName().trim(),
        request.lastName().trim(),
        photo,
        email != null ? email.toLowerCase() : null,
        Strings.blankToNull(request.phone()),
        Strings.blankToNull(request.address()),
        Strings.blankToNull(request.curp()),
        Strings.blankToNull(request.rfc()),
        Strings.blankToNull(request.nss()),
        Strings.blankToNull(request.clabe()),
        Strings.blankToNull(request.position()),
        Strings.blankToNull(request.department()),
        request.contractType(),
        request.workShift(),
        request.salaryPerWeek(),
        request.bonuses(),
        request.foodVouchers(),
        request.integrationFactor());
  }

}
