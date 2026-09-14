package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column; import jakarta.persistence.Entity; import jakarta.persistence.Id; import jakarta.persistence.Table; import jakarta.persistence.Version;
import java.time.Instant; import java.util.UUID;

@Entity @Table(name="pos_cash_movements")
public class PosCashMovementJpaEntity {
  @Id @Column(name="movement_id") private UUID movementId; @Column(name="shift_id") private UUID shiftId;
  @Column(name="headquarter_id") private long headquarterId; @Column(name="event_id") private UUID eventId;
  @Column(name="movement_type") private String movementType; @Column(name="amount_centavos") private long amountCentavos;
  private String folio; private String reason; @Column(name="authorized_by_user_id") private Long authorizedByUserId;
  @Column(name="authorized_by_role") private String authorizedByRole; @Column(name="occurred_at") private Instant occurredAt;
  @Column(name="created_at") private Instant createdAt; @Column(name="updated_at") private Instant updatedAt; @Version private long version;
  protected PosCashMovementJpaEntity() {}
  public PosCashMovementJpaEntity(UUID id, UUID shift, long hq, UUID event, String type, long amount, String folio, String reason, Long user, String role, Instant occurred){movementId=id;shiftId=shift;headquarterId=hq;eventId=event;movementType=type;amountCentavos=amount;this.folio=folio;this.reason=reason;authorizedByUserId=user;authorizedByRole=role;occurredAt=occurred;createdAt=Instant.now();updatedAt=createdAt;version=1;}
  public UUID getMovementId(){return movementId;} public UUID getShiftId(){return shiftId;} public String getMovementType(){return movementType;} public long getAmountCentavos(){return amountCentavos;} public String getFolio(){return folio;} public String getReason(){return reason;} public Instant getOccurredAt(){return occurredAt;}
}
