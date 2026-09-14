package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "pos_shifts")
public class PosShiftJpaEntity {
  @Id @Column(name = "shift_id") private UUID shiftId;
  @Column(name = "headquarter_id", nullable = false) private long headquarterId;
  @Column(name = "device_id", nullable = false) private UUID deviceId;
  @Column(name = "cashier_operator_id") private Long cashierOperatorId;
  @Column(name = "opening_cash_centavos", nullable = false) private long openingCashCentavos;
  @Column(name = "opened_at", nullable = false) private Instant openedAt;
  @Column(name = "closed_at") private Instant closedAt;
  @Column(nullable = false) private String status;
  @Column(name = "closing_expected_cash_centavos") private Long expectedCashCentavos;
  @Column(name = "closing_counted_cash_centavos") private Long countedCashCentavos;
  @Column(name = "closing_difference_centavos") private Long differenceCentavos;
  @Column(name = "opened_event_id", nullable = false) private UUID openedEventId;
  @Column(name = "closed_event_id") private UUID closedEventId;
  @Column(name = "created_at", nullable = false) private Instant createdAt;
  @Column(name = "updated_at", nullable = false) private Instant updatedAt;
  @Version @Column(nullable = false) private long version;
  protected PosShiftJpaEntity() {}
  public PosShiftJpaEntity(UUID id,long hq,UUID device,Long cashier,long opening,Instant opened,UUID event){shiftId=id;headquarterId=hq;deviceId=device;cashierOperatorId=cashier;openingCashCentavos=opening;openedAt=opened;status="OPEN";openedEventId=event;createdAt=Instant.now();updatedAt=createdAt;version=1;}
  public UUID getShiftId(){return shiftId;} public long getHeadquarterId(){return headquarterId;} public UUID getDeviceId(){return deviceId;} public Long getCashierOperatorId(){return cashierOperatorId;} public long getOpeningCashCentavos(){return openingCashCentavos;} public Instant getOpenedAt(){return openedAt;} public Instant getClosedAt(){return closedAt;} public String getStatus(){return status;} public Long getExpectedCashCentavos(){return expectedCashCentavos;} public Long getCountedCashCentavos(){return countedCashCentavos; } public Long getDifferenceCentavos(){return differenceCentavos;}
  public void close(Instant at,long expected,long counted,long difference,UUID event){closedAt=at;status="CLOSED";expectedCashCentavos=expected;countedCashCentavos=counted;differenceCentavos=difference;closedEventId=event;updatedAt=Instant.now();}
}
