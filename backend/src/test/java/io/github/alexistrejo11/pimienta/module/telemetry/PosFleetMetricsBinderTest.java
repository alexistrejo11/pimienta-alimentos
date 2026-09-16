package io.github.alexistrejo11.pimienta.module.telemetry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.PosFleetMetricsBinder;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.TelemetryObservabilityService;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.TelemetryObservabilityService.PosFleetSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class PosFleetMetricsBinderTest {

  @Test
  void exportsBoundedFleetGaugesWithoutDeviceLabels() {
    TelemetryObservabilityService observability = mock(TelemetryObservabilityService.class);
    when(observability.fleet(null)).thenReturn(new PosFleetSnapshot(4, 1, 1, 2, 1, 9, 2, 120));
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    new PosFleetMetricsBinder(observability).bindTo(registry);

    assertEquals(1.0, registry.get("pimienta.pos.devices").tag("state", "online").gauge().value());
    assertEquals(1.0, registry.get("pimienta.pos.devices").tag("state", "degraded").gauge().value());
    assertEquals(2.0, registry.get("pimienta.pos.devices").tag("state", "offline").gauge().value());
    assertEquals(9.0, registry.get("pimienta.pos.pending.events").gauge().value());
    assertEquals(120.0, registry.get("pimienta.pos.oldest.pending.age.seconds").gauge().value());
  }
}
