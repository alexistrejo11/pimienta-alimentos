package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "opportunity_proposals")
public class OpportunityProposalJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "opportunity_id", nullable = false)
  private Long opportunityId;

  @Column(nullable = false, length = 40)
  private String kind;

  @Column(length = 4000)
  private String notes;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

  public String getKind() {
    return kind;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
