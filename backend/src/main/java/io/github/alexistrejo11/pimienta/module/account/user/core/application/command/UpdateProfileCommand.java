package io.github.alexistrejo11.pimienta.module.account.user.core.application.command;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender;

public record UpdateProfileCommand(
        String firstName, String lastName, Gender gender, String phone) {
}
