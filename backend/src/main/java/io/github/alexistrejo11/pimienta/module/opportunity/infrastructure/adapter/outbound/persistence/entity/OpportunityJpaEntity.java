package io.github.alexistrejo11.pimienta.module.opportunity.infrastructure.adapter.outbound.persistence.entity;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "opportunities")
public class OpportunityJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "organization_id", nullable = false)
  private Long organizationId;

  @Column(nullable = false, length = 240)
  private String title;

  @Column(nullable = false, length = 40)
  private String status;

  @Column(name = "source_url", length = 500)
  private String sourceUrl;

  @Column(name = "external_reference", length = 120)
  private String externalReference;

  @Column(name = "published_on")
  private LocalDate publishedOn;

  @Column(name = "submission_deadline")
  private LocalDate submissionDeadline;

  @Column(name = "decision_on")
  private LocalDate decisionOn;

  @Column(length = 4000)
  private String notes;

  @Column(name = "contract_id")
  private Long contractId;

  @Column(name = "submitted_at")
  private LocalDateTime submittedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSourceUrl() {
    return sourceUrl;
  }

  public void setSourceUrl(String sourceUrl) {
    this.sourceUrl = sourceUrl;
  }

  public String getExternalReference() {
    return externalReference;
  }

  public void setExternalReference(String externalReference) {
    this.externalReference = externalReference;
  }

  public LocalDate getPublishedOn() {
    return publishedOn;
  }

  public void setPublishedOn(LocalDate publishedOn) {
    this.publishedOn = publishedOn;
  }

  public LocalDate getSubmissionDeadline() {
    return submissionDeadline;
  }

  public void setSubmissionDeadline(LocalDate submissionDeadline) {
    this.submissionDeadline = submissionDeadline;
  }

  public LocalDate getDecisionOn() {
    return decisionOn;
  }

  public void setDecisionOn(LocalDate decisionOn) {
    this.decisionOn = decisionOn;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public Long getContractId() {
    return contractId;
  }

  public void setContractId(Long contractId) {
    this.contractId = contractId;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public void setSubmittedAt(LocalDateTime submittedAt) {
    this.submittedAt = submittedAt;
  }

  public LocalDateTime getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(LocalDateTime closedAt) {
    this.closedAt = closedAt;
  }
}
