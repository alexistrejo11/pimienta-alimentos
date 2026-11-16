package io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ContractType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.WorkShift;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.BenefitsProfile;

import java.math.BigDecimal;

import org.springframework.web.multipart.MultipartFile;

/**
 * Partial update: each component is applied only when non-null; null keeps the
 * employee's current value.
 */
public record UpdateEmployeeParams(
    Long id,
    String firstName,
    String lastName,
    MultipartFile photo,
    String email,
    String phone,
    String address,
    String curp,
    String rfc,
    String nss,
    String clabe,
    String position,
    String department,
    ContractType contractType,
    WorkShift workShift,
    BigDecimal salaryPerWeek,
    BigDecimal bonuses,
    BigDecimal foodVouchers,
    BigDecimal integrationFactor) {

  /**
   * Merges non-null fields into the aggregate in place (null = leave existing).
   */
  public void applyPartialMerge(Employee employee, String photoUrl) {
    if (firstName != null) {
      employee.setPersonal(employee.getPersonal().setFirstName(firstName));
    }
    if (lastName != null) {
      employee.setPersonal(employee.getPersonal().setLastName(lastName));
    }
    if (photoUrl != null) {
      employee.setPersonal(employee.getPersonal().setPhotoUrl(photoUrl));
    }
    if (email != null) {
      employee.setPersonal(employee.getPersonal().setEmail(email));
    }
    if (phone != null) {
      employee.setPersonal(employee.getPersonal().setPhone(phone));
    }
    if (address != null) {
      employee.setPersonal(employee.getPersonal().setAddress(address));
    }
    if (curp != null) {
      employee.setOfficialIds(employee.getOfficialIds().setCurp(curp));
    }
    if (rfc != null) {
      employee.setOfficialIds(employee.getOfficialIds().setRfc(rfc));
    }
    if (nss != null) {
      employee.setOfficialIds(employee.getOfficialIds().setNss(nss));
    }
    if (clabe != null) {
      employee.setOfficialIds(employee.getOfficialIds().setClabe(clabe));
    }
    if (position != null) {
      employee.setEmployment(employee.getEmployment().setPosition(position));
    }
    if (department != null) {
      employee.setEmployment(employee.getEmployment().setDepartment(department));
    }
    if (contractType != null) {
      employee.setEmployment(employee.getEmployment().setContractType(contractType));
    }
    if (workShift != null) {
      employee.setEmployment(employee.getEmployment().setWorkShift(workShift));
    }
    if (salaryPerWeek != null) {
      employee.setCompensation(employee.getCompensation().setSalaryPerWeek(salaryPerWeek));
    }
    if (bonuses != null) {
      employee.setCompensation(employee.getCompensation().setBonuses(bonuses));
    }
    if (foodVouchers != null) {
      employee.setCompensation(employee.getCompensation().setFoodVouchers(foodVouchers));
    }
    if (integrationFactor != null) {
      var benefits = employee.getBenefits();
      employee.setBenefits(
          new BenefitsProfile(
              integrationFactor,
              benefits.imssWorkerType(),
              benefits.imssSalaryType(),
              benefits.christmasBonusDays(),
              benefits.vacationDays(),
              benefits.vacationPremiumPercent()));
    }
  }
}
