package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        Gender gender,
        String phone,
        LocalDate dateOfBirth,
        AccountStatus accountStatus,
        String bannedReason,
        LocalDateTime bannedAt,
        List<String> roles,
        List<String> permissions,
        List<Long> assignedHeadquarterIds,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
