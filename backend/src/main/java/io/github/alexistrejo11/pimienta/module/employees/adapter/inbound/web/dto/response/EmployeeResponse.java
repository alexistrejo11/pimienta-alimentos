package io.github.alexistrejo11.pimienta.module.employees.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ContractType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.EmployeeStatus;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ImssSalaryType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.ImssWorkerType;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.enums.WorkShift;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String photoUrl,
        String email,
        String phone,
        String address,
        LocalDate birthDate,
        String nationality,
        String curp,
        String rfc,
        String nss,
        String clabe,
        String employeeNumber,
        String position,
        String department,
        ContractType contractType,
        WorkShift workShift,
        LocalDate hireDate,
        LocalDate terminationDate,
        EmployeeStatus status,
        BigDecimal salaryPerWeek,
        BigDecimal bonuses,
        BigDecimal foodVouchers,
        BigDecimal integrationFactor,
        ImssWorkerType imssWorkerType,
        ImssSalaryType imssSalaryType,
        int christmasBonusDays,
        int vacationDays,
        BigDecimal vacationPremiumPercent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public EmployeeResponse {
        firstName = firstName != null ? firstName.trim() : "";
        lastName = lastName != null ? lastName.trim() : "";
        photoUrl = photoUrl != null ? photoUrl.trim() : "";
        email = email != null ? email.trim() : "";
        phone = phone != null ? phone.trim() : "";
        address = address != null ? address.trim() : "";
        nationality = nationality != null ? nationality.trim() : "";
        curp = curp != null ? curp.trim() : "";
        rfc = rfc != null ? rfc.trim() : "";
        nss = nss != null ? nss.trim() : "";
        clabe = clabe != null ? clabe.trim() : "";
        employeeNumber = employeeNumber != null ? employeeNumber.trim() : "";
        position = position != null ? position.trim() : "";
        department = department != null ? department.trim() : "";
        contractType = contractType != null ? contractType : null;
        workShift = workShift != null ? workShift : null;
        hireDate = hireDate != null ? hireDate : null;
        terminationDate = terminationDate != null ? terminationDate : null;
        status = status != null ? status : null;
        salaryPerWeek = salaryPerWeek != null ? salaryPerWeek : null;
        bonuses = bonuses != null ? bonuses : null;
        foodVouchers = foodVouchers != null ? foodVouchers : null;
        integrationFactor = integrationFactor != null ? integrationFactor : null;
        imssWorkerType = imssWorkerType != null ? imssWorkerType : null;
        imssSalaryType = imssSalaryType != null ? imssSalaryType : null;
        vacationPremiumPercent = vacationPremiumPercent != null ? vacationPremiumPercent : null;
        createdAt = createdAt != null ? createdAt : null;
        updatedAt = updatedAt != null ? updatedAt : null;
    }
}
