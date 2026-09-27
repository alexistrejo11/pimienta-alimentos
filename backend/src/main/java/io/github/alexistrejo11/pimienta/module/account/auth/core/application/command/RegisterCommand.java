package io.github.alexistrejo11.pimienta.module.account.auth.core.application.command;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender;

public record RegisterCommand(
                String email,
                String password,
                String firstName,
                String lastName,
                Gender gender,
                String phone) {

        /** Safe for logs: no password, phone only as length/presence. */
        public String toStringMasked() {
                String phonePart =
                                phone == null ? "null" : ("present(len=" + phone.length() + ")");
                return "RegisterCommand(email=%s, password=***, firstName=%s, lastName=%s, gender=%s, phone=%s)"
                                .formatted(email, firstName, lastName, gender, phonePart);
        }

        public RegisterCommand {
                // Data enters clean but assert not null email to avoid NPE
                email = email != null ? email.trim().toLowerCase() : "";
        }
}