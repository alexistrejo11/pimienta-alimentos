package io.github.alexistrejo11.pimienta.module.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.TelemetryIngestionService;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.WebTelemetryEventRequest;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryBatchRequest;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryHealthRequest;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.PosHealthSnapshotRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import io.github.alexistrejo11.pimienta.config.security.DeviceAuthenticationContext;
import java.util.List;
import java.util.UUID;

class TelemetryIngestionServiceTest {

  @Test
  void recordsWebEventWithBoundedSourceMetric() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    TelemetryIngestionService service = new TelemetryIngestionService(registry, mock(PosHealthSnapshotRepository.class));

    service.ingestWeb(new WebTelemetryEventRequest(
        1, "unhandled_exception", "ERROR", "failure", null, "/app", "dev", null, null, null));

    assertEquals(1.0, registry.get("pimienta.telemetry.events.received")
        .tag("source", "web")
        .counter()
        .count());
  }

  @Test
  void persistsPosHealthSnapshotFromExistingBatchContract() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    PosHealthSnapshotRepository repository = mock(PosHealthSnapshotRepository.class);
    TelemetryIngestionService service = new TelemetryIngestionService(registry, repository);
    DeviceAuthenticationContext device = mock(DeviceAuthenticationContext.class);
    UUID deviceId = UUID.randomUUID();
    when(device.deviceId()).thenReturn(deviceId);
    when(device.headquarterId()).thenReturn(7L);

    service.ingestPos(device, new PosTelemetryBatchRequest(List.of(),
        new PosTelemetryHealthRequest("RETRYING", 3, 90, "2.4.0")));

    verify(repository).save(deviceId, 7L, "RETRYING", 3, 90, "2.4.0");
  }
}
