package io.github.alexistrejo11.pimienta.module.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.TelemetryIngestionService;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.WebTelemetryEventRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class TelemetryIngestionServiceTest {

  @Test
  void recordsWebEventWithBoundedSourceMetric() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    TelemetryIngestionService service = new TelemetryIngestionService(registry);

    service.ingestWeb(new WebTelemetryEventRequest(
        1, "unhandled_exception", "ERROR", "failure", null, "/app", "dev", null, null, null));

    assertEquals(1.0, registry.get("pimienta.telemetry.events.received")
        .tag("source", "web")
        .counter()
        .count());
  }
}
