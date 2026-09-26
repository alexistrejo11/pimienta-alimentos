package io.github.alexistrejo11.pimienta.module.account.auth.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender;
import io.github.alexistrejo11.pimienta.shared.validation.PasswordStrength;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(max = 120) String firstName,
    @NotBlank @Size(max = 120) String lastName,
    @NotNull Gender gender,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 32) @Pattern(regexp = "^[+]?[0-9\\s().-]{8,31}$", message = "Phone must be 8–32 characters and contain digits.") String phone,
    @NotBlank @PasswordStrength String password) {
}
