package io.github.alexistrejo11.pimienta.module.employees.core.domain.model;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.BenefitsProfile;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.Compensation;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.Employment;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.OfficialIdentifiers;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.PersonalProfile;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Employee extends BaseDomain<Long> {

  private PersonalProfile personal;
  private OfficialIdentifiers officialIds;
  private Employment employment;
  private Compensation compensation;
  private BenefitsProfile benefits;

  private Employee() {
    this.id = 0L;
    this.personal = PersonalProfile.empty();
    this.officialIds = OfficialIdentifiers.empty();
    this.employment = Employment.empty();
    this.compensation = Compensation.baseline(BigDecimal.ZERO);
    this.benefits = BenefitsProfile.legalDefaults();
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.deletedAt = null;
  }

  public void markAsDeleted() {
    this.deletedAt = LocalDateTime.now();
    touch();
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public PersonalProfile getPersonal() {
    return personal != null ? personal : PersonalProfile.empty();
  }

  public OfficialIdentifiers getOfficialIds() {
    return officialIds != null ? officialIds : OfficialIdentifiers.empty();
  }

  public Employment getEmployment() {
    return employment != null ? employment : Employment.empty();
  }

  public Compensation getCompensation() {
    return compensation != null ? compensation : Compensation.baseline(BigDecimal.ZERO);
  }

  public BenefitsProfile getBenefits() {
    return benefits != null ? benefits : BenefitsProfile.legalDefaults();
  }

  public EmployeeStatus getStatus() {
    Employment em = getEmployment();
    return em.status() != null ? em.status() : EmployeeStatus.INACTIVE;
  }

  public void setPersonal(PersonalProfile personal) {
    this.personal = personal != null ? personal : PersonalProfile.empty();
  }

  public void setOfficialIds(OfficialIdentifiers officialIds) {
    this.officialIds = officialIds != null ? officialIds : OfficialIdentifiers.empty();
  }

  public void setEmployment(Employment employment) {
    this.employment = employment != null ? employment : Employment.empty();
  }

  public void setCompensation(Compensation compensation) {
    this.compensation = compensation != null ? compensation : Compensation.baseline(BigDecimal.ZERO);
  }

  public void setBenefits(BenefitsProfile benefits) {
    this.benefits = benefits != null ? benefits : BenefitsProfile.legalDefaults();
  }

  /** Direct status write; application layer owns termination-date side effects. */
  public void setStatus(EmployeeStatus status) {
    this.employment = getEmployment().setStatus(status != null ? status : EmployeeStatus.INACTIVE);
  }

  public void setHireDate(LocalDate hireDate) {
    this.employment = getEmployment().setHireDate(hireDate);
  }

  public void setTerminationDate(LocalDate terminationDate) {
    this.employment = getEmployment().setTerminationDate(terminationDate);
  }

  public static class SafeBuilder {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;
    private PersonalProfile personal;
    private OfficialIdentifiers officialIds;
    private Employment employment;
    private Compensation compensation;
    private BenefitsProfile benefits;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withPersonal(PersonalProfile personal) {
      this.personal = personal != null ? personal : PersonalProfile.empty();
      return this;
    }

    public SafeBuilder withOfficialIds(OfficialIdentifiers officialIds) {
      this.officialIds = officialIds != null ? officialIds : OfficialIdentifiers.empty();
      return this;
    }

    public SafeBuilder withEmployment(Employment employment) {
      this.employment = employment != null ? employment : Employment.empty();
      return this;
    }

    public SafeBuilder withCompensation(Compensation compensation) {
      this.compensation = compensation != null ? compensation : Compensation.baseline(BigDecimal.ZERO);
      return this;
    }

    public SafeBuilder withBenefits(BenefitsProfile benefits) {
      this.benefits = benefits != null ? benefits : BenefitsProfile.legalDefaults();
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
      return this;
    }

    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder withVersion(Long version) {
      this.version = version;
      return this;
    }

    public Employee build() {
      Employee e = new Employee();
      e.id = id;
      e.createdAt = createdAt;
      e.updatedAt = updatedAt;
      e.deletedAt = deletedAt;
      e.version = version;
      e.personal = personal != null ? personal : PersonalProfile.empty();
      e.officialIds = officialIds != null ? officialIds : OfficialIdentifiers.empty();
      e.employment = employment != null ? employment : Employment.empty();
      e.compensation = compensation != null ? compensation : Compensation.baseline(BigDecimal.ZERO);
      e.benefits = benefits != null ? benefits : BenefitsProfile.legalDefaults();

      return e;
    }
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }
}
