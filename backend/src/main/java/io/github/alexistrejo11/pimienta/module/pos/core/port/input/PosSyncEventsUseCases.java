package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PosSyncEventsUseCases {

  List<EventIngestResult> ingest(UUID authenticatedDeviceId, IngestPosEventsCommand command);

  record EventIngestResult(
      UUID eventId,
      PosEventResultStatus status,
      Instant serverReceivedAt,
      UUID incidentId,
      String message) {}
}
