package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosSyncIncidentAlreadyAcceptedException;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;
import java.util.UUID;

public class PosSyncIncident extends BaseDomain<UUID> {

  private UUID eventId;
  private Long headquarterId;
  private String reasonCode;
  private String detail;
  private LocalDateTime acceptedAt;
  private Long acceptedBy;
  private String acceptLabel;
  private String acceptNote;

  private PosSyncIncident() {
    this.eventId = UUID.randomUUID();
    this.headquarterId = 0L;
    this.reasonCode = "";
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public UUID getEventId() {
    return eventId;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public String getReasonCode() {
    return reasonCode != null ? reasonCode : "";
  }

  public String getDetail() {
    return detail;
  }

  public LocalDateTime getAcceptedAt() {
    return acceptedAt;
  }

  public Long getAcceptedBy() {
    return acceptedBy;
  }

  public String getAcceptLabel() {
    return acceptLabel;
  }

  public String getAcceptNote() {
    return acceptNote;
  }

  public boolean isAccepted() {
    return acceptedAt != null;
  }

  /**
   * Classification-only accept: sets label/note/auditor. Does not mutate sale payload.
   */
  public void accept(Long acceptedByUserId, String label, String note) {
    if (isAccepted()) {
      throw new PosSyncIncidentAlreadyAcceptedException(getId());
    }
    this.acceptedAt = LocalDateTime.now();
    this.acceptedBy = acceptedByUserId;
    this.acceptLabel = label;
    this.acceptNote = note;
    this.updatedAt = this.acceptedAt;
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private UUID id;
    private UUID eventId;
    private Long headquarterId;
    private String reasonCode;
    private String detail;
    private LocalDateTime acceptedAt;
    private Long acceptedBy;
    private String acceptLabel;
    private String acceptNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(UUID id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withEventId(UUID eventId) {
      this.eventId = eventId;
      return this;
    }

    public SafeBuilder withHeadquarterId(Long headquarterId) {
      this.headquarterId = headquarterId;
      return this;
    }

    public SafeBuilder withReasonCode(String reasonCode) {
      this.reasonCode = reasonCode;
      return this;
    }

    public SafeBuilder withDetail(String detail) {
      this.detail = detail;
      return this;
    }

    public SafeBuilder withAcceptedAt(LocalDateTime acceptedAt) {
      this.acceptedAt = acceptedAt;
      return this;
    }

    public SafeBuilder withAcceptedBy(Long acceptedBy) {
      this.acceptedBy = acceptedBy;
      return this;
    }

    public SafeBuilder withAcceptLabel(String acceptLabel) {
      this.acceptLabel = acceptLabel;
      return this;
    }

    public SafeBuilder withAcceptNote(String acceptNote) {
      this.acceptNote = acceptNote;
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

    public PosSyncIncident reconstruct() {
      PosSyncIncident i = new PosSyncIncident();
      i.id = id;
      i.eventId = eventId != null ? eventId : UUID.randomUUID();
      i.headquarterId = headquarterId != null ? headquarterId : 0L;
      i.reasonCode = reasonCode != null ? reasonCode : "";
      i.detail = detail;
      i.acceptedAt = acceptedAt;
      i.acceptedBy = acceptedBy;
      i.acceptLabel = acceptLabel;
      i.acceptNote = acceptNote;
      i.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      i.updatedAt = updatedAt != null ? updatedAt : i.createdAt;
      i.deletedAt = deletedAt;
      i.version = version != null ? version : 0L;
      return i;
    }

    public PosSyncIncident open() {
      PosSyncIncident i = reconstruct();
      if (i.id == null) {
        i.id = UUID.randomUUID();
      }
      LocalDateTime now = LocalDateTime.now();
      i.createdAt = now;
      i.updatedAt = now;
      i.acceptedAt = null;
      i.acceptedBy = null;
      i.acceptLabel = null;
      i.acceptNote = null;
      i.version = null;
      return i;
    }
  }
}
