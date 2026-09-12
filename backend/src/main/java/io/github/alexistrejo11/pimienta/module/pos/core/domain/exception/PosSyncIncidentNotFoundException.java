package io.github.alexistrejo11.pimienta.module.pos.core.domain.exception;

import io.github.alexistrejo11.pimienta.shared.exception.ErrorCode;
import io.github.alexistrejo11.pimienta.shared.exception.ResourceNotFoundException;
import java.util.Map;
import java.util.UUID;

public class PosSyncIncidentNotFoundException extends ResourceNotFoundException {

  public PosSyncIncidentNotFoundException(UUID id) {
    super(
        ErrorCode.POS_SYNC_INCIDENT_NOT_FOUND,
        "The requested POS sync incident was not found.",
        Map.of("incidentId", id.toString()),
        "POS sync incident not found: id=" + id);
  }
}
