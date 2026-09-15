package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public class PosSyncEvent extends BaseDomain<UUID> {

  private String eventType;
  private int schemaVersion;
  private UUID deviceId;
  private Long headquarterId;
  private long deviceSequence;
  private UUID aggregateId;
  private UUID shiftId;
  private Instant occurredAt;
  private String payloadJson;
  private PosEventResultStatus status;
  private Instant serverReceivedAt;
  private UUID incidentId;
  private String message;

  private PosSyncEvent() {
    this.eventType = "";
    this.schemaVersion = 1;
    this.deviceId = UUID.randomUUID();
    this.headquarterId = 0L;
    this.deviceSequence = 0L;
    this.occurredAt = Instant.now();
    this.payloadJson = "{}";
    this.status = PosEventResultStatus.ACCEPTED;
    this.serverReceivedAt = Instant.now();
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public String getEventType() {
    return eventType != null ? eventType : "";
  }

  public int getSchemaVersion() {
    return schemaVersion;
  }

  public UUID getDeviceId() {
    return deviceId;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public long getDeviceSequence() {
    return deviceSequence;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public UUID getShiftId() {
    return shiftId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getPayloadJson() {
    return payloadJson != null ? payloadJson : "{}";
  }

  public PosEventResultStatus getStatus() {
    return status != null ? status : PosEventResultStatus.ACCEPTED;
  }

  public Instant getServerReceivedAt() {
    return serverReceivedAt;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public String getMessage() {
    return message;
  }

  public void attachIncident(UUID incidentId, PosEventResultStatus status, String message) {
    this.incidentId = incidentId;
    this.status = status;
    this.message = message;
    touch();
  }

  /**
   * After Superadmin accept: flip review status so the fact is included in ACCEPTED-only reports.
   * Does not mutate sale payload.
   */
  public void markAcceptedAfterReview(String message) {
    if (this.status != PosEventResultStatus.REQUIRES_REVIEW) {
      throw new IllegalStateException(
          "Only REQUIRES_REVIEW events can be marked ACCEPTED after review: eventId="
              + getId()
              + " status="
              + this.status);
    }
    this.status = PosEventResultStatus.ACCEPTED;
    this.message = message;
    touch();
  }

  public void markRejected(String message) {
    this.status = PosEventResultStatus.REJECTED;
    this.message = message;
    touch();
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private UUID id;
    private String eventType;
    private Integer schemaVersion;
    private UUID deviceId;
    private Long headquarterId;
    private Long deviceSequence;
    private UUID aggregateId;
    private UUID shiftId;
    private Instant occurredAt;
    private String payloadJson;
    private PosEventResultStatus status;
    private Instant serverReceivedAt;
    private UUID incidentId;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(UUID id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withEventType(String eventType) {
      this.eventType = eventType;
      return this;
    }

    public SafeBuilder withSchemaVersion(Integer schemaVersion) {
      this.schemaVersion = schemaVersion;
      return this;
    }

    public SafeBuilder withDeviceId(UUID deviceId) {
      this.deviceId = deviceId;
      return this;
    }

    public SafeBuilder withHeadquarterId(Long headquarterId) {
      this.headquarterId = headquarterId;
      return this;
    }

    public SafeBuilder withDeviceSequence(Long deviceSequence) {
      this.deviceSequence = deviceSequence;
      return this;
    }

    public SafeBuilder withAggregateId(UUID aggregateId) {
      this.aggregateId = aggregateId;
      return this;
    }

    public SafeBuilder withShiftId(UUID shiftId) {
      this.shiftId = shiftId;
      return this;
    }

    public SafeBuilder withOccurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public SafeBuilder withPayloadJson(String payloadJson) {
      this.payloadJson = payloadJson;
      return this;
    }

    public SafeBuilder withStatus(PosEventResultStatus status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withServerReceivedAt(Instant serverReceivedAt) {
      this.serverReceivedAt = serverReceivedAt;
      return this;
    }

    public SafeBuilder withIncidentId(UUID incidentId) {
      this.incidentId = incidentId;
      return this;
    }

    public SafeBuilder withMessage(String message) {
      this.message = message;
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder withVersion(Long version) {
      this.version = version;
      return this;
    }

    public PosSyncEvent reconstruct() {
      PosSyncEvent e = new PosSyncEvent();
      e.id = id;
      e.eventType = eventType != null ? eventType : "";
      e.schemaVersion = schemaVersion != null ? schemaVersion : 1;
      e.deviceId = deviceId != null ? deviceId : UUID.randomUUID();
      e.headquarterId = headquarterId != null ? headquarterId : 0L;
      e.deviceSequence = deviceSequence != null ? deviceSequence : 0L;
      e.aggregateId = aggregateId;
      e.shiftId = shiftId;
      e.occurredAt = occurredAt != null ? occurredAt : Instant.now();
      e.payloadJson = payloadJson != null ? payloadJson : "{}";
      e.status = status != null ? status : PosEventResultStatus.ACCEPTED;
      e.serverReceivedAt = serverReceivedAt != null ? serverReceivedAt : Instant.now();
      e.incidentId = incidentId;
      e.message = message;
      e.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      e.updatedAt = updatedAt != null ? updatedAt : e.createdAt;
      e.deletedAt = deletedAt;
      e.version = version != null ? version : 0L;
      return e;
    }

    public PosSyncEvent register() {
      PosSyncEvent e = reconstruct();
      if (e.id == null) {
        throw new IllegalArgumentException("eventId is required");
      }
      Instant now = Instant.now();
      LocalDateTime localNow = LocalDateTime.now();
      e.serverReceivedAt = now;
      e.createdAt = localNow;
      e.updatedAt = localNow;
      e.version = null;
      return e;
    }
  }
}
