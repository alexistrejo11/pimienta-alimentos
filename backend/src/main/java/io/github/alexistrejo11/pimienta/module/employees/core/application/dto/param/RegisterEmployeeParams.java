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
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.multipart.MultipartFile;

/**
 * Application parameters for employee self-registration / onboarding draft.
 * Assembles the aggregate via.
 */
@Builder
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
                PersonalProfile personal = PersonalProfile.builder()
                                .firstName(firstName)
                                .lastName(lastName)
                                .photoUrl(photoUrl)
                                .email(safeEmail != null ? safeEmail : "")
                                .phone(phone != null ? phone : "")
                                .address(address != null ? address : "")
                                .birthDate(birthDate)
                                .nationality("Mexicana")
                                .build();

                OfficialIdentifiers officialIds = OfficialIdentifiers.builder()
                                .curp(curp != null ? curp : "")
                                .rfc(rfc != null ? rfc : "")
                                .nss(nss != null ? nss : "")
                                .clabe(clabe != null ? clabe : "")
                                .employeeNumber(employeeNumber != null ? employeeNumber : "")
                                .build();

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
                                .withVersion(1L)
                                .build();
        }
}
