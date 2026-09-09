package io.github.alexistrejo11.pimienta.module.pos.core.application.command;

import java.util.UUID;

public record AcceptPosSyncIncidentCommand(
    UUID incidentId, long acceptedByUserId, String label, String note) {}
