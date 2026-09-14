package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity;

import jakarta.persistence.Column; import jakarta.persistence.Entity; import jakarta.persistence.Id; import jakarta.persistence.Table; import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode; import org.hibernate.type.SqlTypes; import java.time.Instant; import java.util.UUID;

@Entity @Table(name="pos_cash_counts")
public class PosCashCountJpaEntity {
  @Id @Column(name="count_id") private UUID countId; @Column(name="shift_id") private UUID shiftId; @Column(name="headquarter_id") private long headquarterId; @Column(name="event_id") private UUID eventId; @Column(name="total_centavos") private long totalCentavos; @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false) private String denominations; @Column(name="submitted_at") private Instant submittedAt; @Column(name="created_at") private Instant createdAt; @Column(name="updated_at") private Instant updatedAt; @Version private long version;
  protected PosCashCountJpaEntity() {}
  public PosCashCountJpaEntity(UUID id, UUID shift, long hq, UUID event, long total, String denominations, Instant submitted){countId=id;shiftId=shift;headquarterId=hq;eventId=event;totalCentavos=total;this.denominations=denominations;submittedAt=submitted;createdAt=Instant.now();updatedAt=createdAt;version=1;}
  public UUID getCountId(){return countId;} public UUID getShiftId(){return shiftId;} public long getTotalCentavos(){return totalCentavos;} public String getDenominations(){return denominations;} public Instant getSubmittedAt(){return submittedAt;}
}
