package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "opportunity_requirements")
public class OpportunityRequirementJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "opportunity_id", nullable = false)
  private Long opportunityId;

  @Column(nullable = false, length = 300)
  private String title;

  @Column(nullable = false, length = 40)
  private String category;

  @Column(nullable = false)
  private boolean mandatory;

  @Column(nullable = false, length = 40)
  private String status;

  @Column(name = "due_on")
  private LocalDate dueOn;

  @Column(length = 2000)
  private String notes;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

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

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getDueOn() {
    return dueOn;
  }

  public void setDueOn(LocalDate dueOn) {
    this.dueOn = dueOn;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }
}
