package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

/** Exports low-cardinality POS fleet gauges from the latest accepted health snapshots. */
@Component
public class PosFleetMetricsBinder implements MeterBinder {

  private final TelemetryObservabilityService observability;

  private volatile long cacheExpiresAtEpochMillis;
  private volatile TelemetryObservabilityService.PosFleetSnapshot cached;

  public PosFleetMetricsBinder(TelemetryObservabilityService observability) {
    this.observability = observability;
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder("pimienta.pos.devices", this, binder -> binder.fleet().onlineDevices())
        .tag("state", "online")
        .register(registry);
    Gauge.builder("pimienta.pos.devices", this, binder -> binder.fleet().degradedDevices())
        .tag("state", "degraded")
        .register(registry);
    Gauge.builder("pimienta.pos.devices", this, binder -> binder.fleet().offlineDevices())
        .tag("state", "offline")
        .register(registry);
    Gauge.builder("pimienta.pos.pending.events", this, binder -> binder.fleet().pendingEvents())
        .register(registry);
    Gauge.builder("pimienta.pos.oldest.pending.age.seconds", this,
        binder -> binder.fleet().maxOldestPendingAgeSeconds()).register(registry);
  }

  private TelemetryObservabilityService.PosFleetSnapshot fleet() {
    long now = System.currentTimeMillis();
    TelemetryObservabilityService.PosFleetSnapshot current = cached;
    if (current != null && now < cacheExpiresAtEpochMillis) {
      return current;
    }
    current = observability.fleet(null);
    cached = current;
    cacheExpiresAtEpochMillis = now + 10_000;
    return current;
  }
}
