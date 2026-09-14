package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession.CountType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession.Status;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="inventory_count_sessions")
public class InventoryCountSessionJpaEntity {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="location_id",nullable=false) Long locationId;
  @Enumerated(EnumType.STRING) @Column(name="count_type",nullable=false) CountType countType;
  @Enumerated(EnumType.STRING) @Column(nullable=false) Status status; @Column(name="created_by_id",nullable=false) Long createdById;
  Long submittedById, approvedById; LocalDateTime createdAt,submittedAt,approvedAt,cancelledAt;
  public Long getId(){return id;} public void setId(Long v){id=v;} public Long getLocationId(){return locationId;} public void setLocationId(Long v){locationId=v;}
  public CountType getCountType(){return countType;} public void setCountType(CountType v){countType=v;} public Status getStatus(){return status;} public void setStatus(Status v){status=v;}
  public Long getCreatedById(){return createdById;} public void setCreatedById(Long v){createdById=v;} public Long getSubmittedById(){return submittedById;} public void setSubmittedById(Long v){submittedById=v;}
  public Long getApprovedById(){return approvedById;} public void setApprovedById(Long v){approvedById=v;} public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
  public LocalDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(LocalDateTime v){submittedAt=v;} public LocalDateTime getApprovedAt(){return approvedAt;} public void setApprovedAt(LocalDateTime v){approvedAt=v;} public LocalDateTime getCancelledAt(){return cancelledAt;} public void setCancelledAt(LocalDateTime v){cancelledAt=v;}
}
