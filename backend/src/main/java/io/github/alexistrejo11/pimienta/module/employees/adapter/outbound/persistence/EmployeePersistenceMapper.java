package io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence;

import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.EmployeeBenefitsEmbeddable;
import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.EmployeeCompensationEmbeddable;
import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.EmployeeEmploymentEmbeddable;
import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.EmployeeJpaEntity;
import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.EmployeeOfficialIdsEmbeddable;
import io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.persistence.model.EmployeePersonalEmbeddable;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.BenefitsProfile;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.Compensation;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.Employment;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.OfficialIdentifiers;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.PersonalProfile;

import java.math.BigDecimal;

public class EmployeePersistenceMapper {

  private EmployeePersistenceMapper() {
  }

  public static EmployeeJpaEntity toJpa(Employee domain) {
    EmployeeJpaEntity e = new EmployeeJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }

    EmployeePersonalEmbeddable p = e.getPersonal();
    p.setFirstName(domain.getPersonal().firstName());
    p.setLastName(domain.getPersonal().lastName());
    p.setPhotoUrl(blankToNull(domain.getPersonal().photoUrl()));
    p.setEmail(blankToNull(domain.getPersonal().email()));
    p.setPhone(blankToNull(domain.getPersonal().phone()));
    p.setAddress(blankToNull(domain.getPersonal().address()));
    p.setBirthDate(domain.getPersonal().birthDate());
    p.setNationality(blankToNull(domain.getPersonal().nationality()));

    EmployeeOfficialIdsEmbeddable o = e.getOfficialIds();
    o.setCurp(blankToNull(domain.getOfficialIds().curp()));
    o.setRfc(blankToNull(domain.getOfficialIds().rfc()));
    o.setNss(blankToNull(domain.getOfficialIds().nss()));
    o.setClabe(blankToNull(domain.getOfficialIds().clabe()));
    o.setEmployeeNumber(blankToNull(domain.getOfficialIds().employeeNumber()));

    EmployeeEmploymentEmbeddable em = e.getEmployment();
    em.setPosition(blankToNull(domain.getEmployment().position()));
    em.setDepartment(blankToNull(domain.getEmployment().department()));
    em.setContractType(domain.getEmployment().contractType());
    em.setWorkShift(domain.getEmployment().workShift());
    em.setHireDate(domain.getEmployment().hireDate());
    em.setTerminationDate(domain.getEmployment().terminationDate());
    e.setStatus(domain.getStatus());

    EmployeeCompensationEmbeddable c = e.getCompensation();
    c.setSalaryPerWeek(domain.getCompensation().salaryPerWeek());
    c.setBonuses(domain.getCompensation().bonuses());
    c.setFoodVouchers(domain.getCompensation().foodVouchers());

    EmployeeBenefitsEmbeddable b = e.getBenefits();
    b.setIntegrationFactor(domain.getBenefits().integrationFactor());
    b.setImssWorkerType(domain.getBenefits().imssWorkerType());
    b.setImssSalaryType(domain.getBenefits().imssSalaryType());
    b.setChristmasBonusDays(domain.getBenefits().christmasBonusDays());
    b.setVacationDays(domain.getBenefits().vacationDays());
    b.setVacationPremiumPercent(domain.getBenefits().vacationPremiumPercent());

    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion() != null ? domain.getVersion() : 0L);

    e.fillCreatedAndUpdatedIfNull();
    e.fillUpdatedIfNull();
    e.normalizeVersionIfNull();
    return e;
  }

  public static Employee toDomain(EmployeeJpaEntity e) {
    PersonalProfile personal = toPersonal(e.getPersonal());
    OfficialIdentifiers official = toOfficialIds(e.getOfficialIds());
    Employment employment = toEmployment(e);
    Compensation compensation = toCompensation(e.getCompensation());
    BenefitsProfile benefits = toBenefits(e.getBenefits());

    return Employee.builder()
        .withId(e.getId())
        .withCreatedAt(e.getCreatedAt())
        .withUpdatedAt(e.getUpdatedAt())
        .withDeletedAt(e.getDeletedAt())
        .withVersion(e.getVersion())
        .withPersonal(personal)
        .withOfficialIds(official)
        .withEmployment(employment)
        .withCompensation(compensation)
        .withBenefits(benefits)
        .build();
  }

  private static PersonalProfile toPersonal(EmployeePersonalEmbeddable row) {
    if (row == null) {
      return PersonalProfile.empty();
    }
    return new PersonalProfile(
        row.getFirstName(),
        row.getLastName(),
        row.getPhotoUrl(),
        row.getEmail(),
        row.getPhone(),
        row.getAddress(),
        row.getBirthDate(),
        row.getNationality() != null && !row.getNationality().isBlank()
            ? row.getNationality()
            : "Unknown");
  }

  private static OfficialIdentifiers toOfficialIds(EmployeeOfficialIdsEmbeddable row) {
    if (row == null) {
      return OfficialIdentifiers.empty();
    }
    return new OfficialIdentifiers(
        row.getCurp(), row.getRfc(), row.getNss(), row.getClabe(), row.getEmployeeNumber());
  }

  private static Employment toEmployment(EmployeeJpaEntity entity) {
    EmployeeEmploymentEmbeddable row = entity.getEmployment();
    if (row == null) {
      return Employment.empty();
    }
    return new Employment(
        row.getPosition(),
        row.getDepartment(),
        row.getContractType(),
        row.getWorkShift(),
        row.getHireDate(),
        row.getTerminationDate(),
        entity.getStatus());
  }

  private static Compensation toCompensation(EmployeeCompensationEmbeddable row) {
    if (row == null) {
      return Compensation.baseline(BigDecimal.ZERO);
    }
    return new Compensation(row.getSalaryPerWeek(), row.getBonuses(), row.getFoodVouchers());
  }

  private static BenefitsProfile toBenefits(EmployeeBenefitsEmbeddable row) {
    if (row == null) {
      return BenefitsProfile.legalDefaults();
    }
    return new BenefitsProfile(
        row.getIntegrationFactor(),
        row.getImssWorkerType(),
        row.getImssSalaryType(),
        row.getChristmasBonusDays(),
        row.getVacationDays(),
        row.getVacationPremiumPercent());
  }

  private static String blankToNull(String s) {
    if (s == null) {
      return null;
    }
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }
}
