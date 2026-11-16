package io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ContractType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeOnboardingPhase;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.WorkShift;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.BenefitsProfile;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.Compensation;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.Employment;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.OfficialIdentifiers;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.valueobject.PersonalProfile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

/**
 * Application parameters for employee self-registration / onboarding draft.
 * Assembles the aggregate via.
 */
public record RegisterEmployeeParams(
                String firstName,
                String lastName,
                String email,
                String phone,
                String address,
                String curp,
                String rfc,
                String nss,
                String clabe,
                String employeeNumber,
                String position,
                String department,
                ContractType contractType,
                WorkShift workShift,
                BigDecimal salaryPerWeek,
                LocalDate birthDate,
                EmployeeOnboardingPhase onboardingPhase,
                MultipartFile photo) {

        public Employee toEmployee(String photoUrl) {
                LocalDateTime now = LocalDateTime.now();
                EmployeeStatus initialStatus = onboardingPhase != null
                                ? onboardingPhase.toEmploymentStatus()
                                : EmployeeStatus.DRAFT;

                String safeEmail = email != null ? email.strip() : null;
                PersonalProfile personal =
                    new PersonalProfile(
                        firstName,
                        lastName,
                        photoUrl,
                        safeEmail != null ? safeEmail : "",
                        phone != null ? phone : "",
                        address != null ? address : "",
                        birthDate,
                        "Mexicana");

                OfficialIdentifiers officialIds =
                    new OfficialIdentifiers(
                        curp != null ? curp : "",
                        rfc != null ? rfc : "",
                        nss != null ? nss : "",
                        clabe != null ? clabe : "",
                        employeeNumber != null ? employeeNumber : "");

                Employment employment = new Employment(
                                position != null ? position : "",
                                department != null ? department : "",
                                contractType != null ? contractType : ContractType.UNDEFINED,
                                workShift != null ? workShift : WorkShift.UNDEFINED,
                                null,
                                null,
                                initialStatus);

                BigDecimal weekly = salaryPerWeek != null ? salaryPerWeek : BigDecimal.ZERO;
                Compensation compensation = Compensation.baseline(weekly);

                return Employee.builder()
                                .withPersonal(personal)
                                .withOfficialIds(officialIds)
                                .withEmployment(employment)
                                .withCompensation(compensation)
                                .withBenefits(BenefitsProfile.legalDefaults())
                                .withCreatedAt(now)
                                .withUpdatedAt(now)
                                .withDeletedAt(null)
                                .build();
        }
}
