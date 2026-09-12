package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "pos_sync_events")
public class PosSyncEventJpaEntity implements Persistable<UUID> {

  @Id
  @Column(name = "event_id")
  private UUID eventId;

  @Transient private boolean newEntity = true;

  @Column(name = "event_type", nullable = false, length = 64)
  private String eventType;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @Column(name = "device_id", nullable = false)
  private UUID deviceId;

  @Column(name = "headquarter_id", nullable = false)
  private Long headquarterId;

  @Column(name = "device_sequence", nullable = false)
  private long deviceSequence;

  @Column(name = "aggregate_id")
  private UUID aggregateId;

  @Column(name = "shift_id")
  private UUID shiftId;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PosEventResultStatus status;

  @Column(name = "server_received_at", nullable = false)
  private Instant serverReceivedAt;

  @Column(name = "incident_id")
  private UUID incidentId;

  @Column(length = 512)
  private String message;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  @Override
  public UUID getId() {
    return eventId;
  }

  @Override
  public boolean isNew() {
    return newEntity;
  }

  public void setNewEntity(boolean newEntity) {
    this.newEntity = newEntity;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public int getSchemaVersion() {
    return schemaVersion;
  }

  public void setSchemaVersion(int schemaVersion) {
    this.schemaVersion = schemaVersion;
  }

  public UUID getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(UUID deviceId) {
    this.deviceId = deviceId;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public long getDeviceSequence() {
    return deviceSequence;
  }

  public void setDeviceSequence(long deviceSequence) {
    this.deviceSequence = deviceSequence;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public void setAggregateId(UUID aggregateId) {
    this.aggregateId = aggregateId;
  }

  public UUID getShiftId() {
    return shiftId;
  }

  public void setShiftId(UUID shiftId) {
    this.shiftId = shiftId;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(Instant occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public PosEventResultStatus getStatus() {
    return status;
  }

  public void setStatus(PosEventResultStatus status) {
    this.status = status;
  }

  public Instant getServerReceivedAt() {
    return serverReceivedAt;
  }

  public void setServerReceivedAt(Instant serverReceivedAt) {
    this.serverReceivedAt = serverReceivedAt;
  }

  public UUID getIncidentId() {
    return incidentId;
  }

  public void setIncidentId(UUID incidentId) {
    this.incidentId = incidentId;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public LocalDateTime getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(LocalDateTime deletedAt) {
    this.deletedAt = deletedAt;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
