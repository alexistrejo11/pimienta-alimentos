package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.config.security.DeviceAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryBatchRequest;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryHealthRequest;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.PosTelemetryLogRequest;
import io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.inbound.web.dto.WebTelemetryEventRequest;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Normalizes client reports into safe structured logs and bounded metrics. */
@Service
public class TelemetryIngestionService {

  private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionService.class);
  private static final int MAX_TEXT_LENGTH = 2000;
  private final MeterRegistry meterRegistry;

  public TelemetryIngestionService(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void ingestWeb(WebTelemetryEventRequest event) {
    meterRegistry.counter("pimienta.telemetry.events.received", "source", "web").increment();
    logEvent("web-client", event.schemaVersion(), event.eventType(), event.level(), event.message(), event.stack(),
        event.occurredAt(), null, null, event.route(), event.release(), event.traceId());
  }

  public int ingestPos(DeviceAuthenticationContext device, PosTelemetryBatchRequest batch) {
    for (PosTelemetryLogRequest event : batch.events()) {
      meterRegistry.counter("pimienta.telemetry.events.received", "source", "pos").increment();
      logEvent("pos-client", event.schemaVersion(), event.eventType(), event.level(), event.message(), event.stack(),
          event.occurredAt(), device.deviceId().toString(), device.headquarterId(), null, null, null);
    }
    PosTelemetryHealthRequest health = batch.health();
    if (health != null) {
      meterRegistry.counter("pimienta.telemetry.health.received", "state", state(health.syncState())).increment();
      log.atInfo()
          .addKeyValue("source", "pos-client")
          .addKeyValue("eventType", "health_snapshot")
          .addKeyValue("deviceId", device.deviceId().toString())
          .addKeyValue("siteId", device.headquarterId())
          .addKeyValue("syncState", state(health.syncState()))
          .addKeyValue("pendingEvents", health.pendingEvents())
          .addKeyValue("oldestPendingAgeSeconds", health.oldestPendingAgeSeconds())
          .addKeyValue("appVersion", clean(health.appVersion(), 40))
          .log("client_telemetry");
    }
    return batch.events().size();
  }

  private void logEvent(String source, int schemaVersion, String eventType, String level, String message, String stack,
      String occurredAt, String deviceId, Long siteId, String route, String release, String traceId) {
    var event = log.atLevel(org.slf4j.event.Level.valueOf(level.toUpperCase(Locale.ROOT)))
        .addKeyValue("source", source)
        .addKeyValue("schemaVersion", schemaVersion)
        .addKeyValue("eventType", clean(eventType, 40))
        .addKeyValue("message", clean(message, MAX_TEXT_LENGTH));
    if (stack != null && !stack.isBlank()) event = event.addKeyValue("stack", clean(stack, 12000));
    if (occurredAt != null) event = event.addKeyValue("occurredAt", clean(occurredAt, 500));
    if (deviceId != null) event = event.addKeyValue("deviceId", deviceId);
    if (siteId != null) event = event.addKeyValue("siteId", siteId);
    if (route != null) event = event.addKeyValue("route", clean(route, 200));
    if (release != null) event = event.addKeyValue("release", clean(release, 80));
    if (traceId != null) event = event.addKeyValue("clientTraceId", clean(traceId, 120));
    event.log("client_telemetry");
  }

  private static String clean(String value, int maxLength) {
    if (value == null) return null;
    String singleLine = value.replace('\n', ' ').replace('\r', ' ').trim();
    return singleLine.length() <= maxLength ? singleLine : singleLine.substring(0, maxLength);
  }

  private static String state(String value) {
    String normalized = clean(value, 30).toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
    return switch (normalized) {
      case "ONLINE", "RETRYING", "REQUIRES_REENROLLMENT", "NOT_CONFIGURED" -> normalized;
      default -> "UNKNOWN";
    };
  }
}
