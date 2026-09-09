package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ConflictException;
import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

public class PosSyncIncidentAlreadyAcceptedException extends ConflictException {

  public PosSyncIncidentAlreadyAcceptedException(UUID id) {
    super(
        ErrorCode.POS_SYNC_INCIDENT_ALREADY_ACCEPTED,
        "This POS sync incident was already accepted.",
        Map.of("incidentId", id.toString()),
        "POS sync incident already accepted: id=" + id);
  }
}
