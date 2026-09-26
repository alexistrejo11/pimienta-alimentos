package io.github.alexistrejo11.pimienta.config.logger;

import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class AuditLogger {
  private static final Logger log = LoggerFactory.getLogger("audit");
  public void logAuditEvent(AuditEvent auditEvent) {
    // Keep `service`/`source` for logging.structured.json.add; use `channel` for origin.
    log.atInfo()
        .addKeyValue("channel", "audit")
        .addKeyValue("eventId", auditEvent.getEventID())
        .addKeyValue("timestamp", auditEvent.getTimeStamp())
        .addKeyValue("method", auditEvent.getMethod())
        .addKeyValue("endpoint", auditEvent.getEndpoint())
        .addKeyValue("operation", auditEvent.getOperation())
        .addKeyValue("userId", auditEvent.getUserID())
        .addKeyValue("clientIp", auditEvent.getClientIP())
        .addKeyValue("userAgent", auditEvent.getUserAgent())
        .addKeyValue("statusCode", auditEvent.getStatusCode())
        .addKeyValue("durationMs", auditEvent.getDurationMs())
        .addKeyValue("success", auditEvent.isSuccess())
        .addKeyValue("metadata", auditEvent.getMetadata())
        .log("audit_event");
  }
}
