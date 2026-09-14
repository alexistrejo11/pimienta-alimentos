package io.github.alexistrejo11.pimienta.module.telemetry.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pos_health_snapshots")
public class PosHealthSnapshotJpaEntity {

  @Id private UUID id;
  @Column(name = "device_id", nullable = false) private UUID deviceId;
  @Column(name = "headquarter_id", nullable = false) private Long headquarterId;
  @Column(name = "sync_state", nullable = false, length = 32) private String syncState;
  @Column(name = "pending_events", nullable = false) private int pendingEvents;
  @Column(name = "oldest_pending_age_seconds", nullable = false) private long oldestPendingAgeSeconds;
  @Column(name = "app_version", nullable = false, length = 64) private String appVersion;
  @Column(name = "received_at", nullable = false) private Instant receivedAt;

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  public UUID getDeviceId() { return deviceId; }
  public void setDeviceId(UUID deviceId) { this.deviceId = deviceId; }
  public Long getHeadquarterId() { return headquarterId; }
  public void setHeadquarterId(Long headquarterId) { this.headquarterId = headquarterId; }
  public String getSyncState() { return syncState; }
  public void setSyncState(String syncState) { this.syncState = syncState; }
  public int getPendingEvents() { return pendingEvents; }
  public void setPendingEvents(int pendingEvents) { this.pendingEvents = pendingEvents; }
  public long getOldestPendingAgeSeconds() { return oldestPendingAgeSeconds; }
  public void setOldestPendingAgeSeconds(long value) { this.oldestPendingAgeSeconds = value; }
  public String getAppVersion() { return appVersion; }
  public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
  public Instant getReceivedAt() { return receivedAt; }
  public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }
}
