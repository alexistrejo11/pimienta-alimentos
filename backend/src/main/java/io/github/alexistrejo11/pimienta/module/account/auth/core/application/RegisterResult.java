package io.github.alexistrejo11.pimienta.module.account.auth.core.application;

/**
 * Outcome of user registration. Presentation maps flags to client-facing copy; no UI strings here.
 */
public record RegisterResult(boolean requireAdminActivation) {}
