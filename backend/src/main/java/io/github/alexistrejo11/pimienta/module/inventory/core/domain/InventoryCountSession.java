package io.github.alexistrejo11.pimienta.module.inventory.core.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryCountSession {
  public enum CountType { FULL, PARTIAL }
  public enum Status { DRAFT, SUBMITTED, APPROVED, CANCELLED }
  private Long id; private Long locationId; private CountType countType; private Status status = Status.DRAFT;
  private Long createdById; private Long submittedById; private Long approvedById;
  private LocalDateTime createdAt, submittedAt, approvedAt, cancelledAt;
  private List<InventoryCountResponse> responses = new ArrayList<>();
  public static InventoryCountSession open(long locationId, CountType type, long creator) { var s = new InventoryCountSession(); s.locationId=locationId; s.countType=type; s.createdById=creator; s.createdAt=LocalDateTime.now(); return s; }
  public void addResponse(InventoryCountResponse r) { responses.add(r); }
  public void replaceResponsesForLoad(List<InventoryCountResponse> r) { responses = new ArrayList<>(r); }
  public List<InventoryCountResponse> getResponses() { return Collections.unmodifiableList(responses); }
  public Long getId() { return id; } public void setId(Long v) { id=v; }
  public Long getLocationId() { return locationId; } public void setLocationId(Long v) { locationId=v; }
  public CountType getCountType() { return countType; } public void setCountType(CountType v) { countType=v; }
  public Status getStatus() { return status; } public void setStatus(Status v) { status=v; }
  public Long getCreatedById() { return createdById; } public void setCreatedById(Long v) { createdById=v; }
  public Long getSubmittedById() { return submittedById; } public void setSubmittedById(Long v) { submittedById=v; }
  public Long getApprovedById() { return approvedById; } public void setApprovedById(Long v) { approvedById=v; }
  public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime v) { createdAt=v; }
  public LocalDateTime getSubmittedAt() { return submittedAt; } public void setSubmittedAt(LocalDateTime v) { submittedAt=v; }
  public LocalDateTime getApprovedAt() { return approvedAt; } public void setApprovedAt(LocalDateTime v) { approvedAt=v; }
  public LocalDateTime getCancelledAt() { return cancelledAt; } public void setCancelledAt(LocalDateTime v) { cancelledAt=v; }
}
