package io.github.alexistrejo11.pimienta.module.pos.core.application.command;

import java.util.UUID;

public record EnrollDeviceCommand(
    String enrollmentCode, UUID devicePublicId, String deviceName, String appVersion) {}
