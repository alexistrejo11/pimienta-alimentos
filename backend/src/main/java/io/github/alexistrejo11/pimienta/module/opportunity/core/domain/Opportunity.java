package io.github.alexistrejo11.pimienta.module.opportunity.core.domain;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.OpportunityStatus;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import io.github.alexistrejo11.pimienta.shared.util.Strings;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Opportunity extends BaseDomain<Long> {

  private Long organizationId;
  private String title;
  private OpportunityStatus status;
  private String sourceUrl;
  private String externalReference;
  private LocalDate publishedOn;
  private LocalDate submissionDeadline;
  private LocalDate decisionOn;
  private String notes;
  private Long contractId;
  private LocalDateTime submittedAt;
  private LocalDateTime closedAt;

  private Opportunity() {
    this.id = 0L;
    this.organizationId = 0L;
    this.title = "";
    this.status = OpportunityStatus.DRAFT;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public Long getOrganizationId() {
    return organizationId != null ? organizationId : 0L;
  }

  public String getTitle() {
    return title != null ? title : "";
  }

  public OpportunityStatus getStatus() {
    return status != null ? status : OpportunityStatus.UNDEFINED;
  }

  public String getSourceUrl() {
    return sourceUrl;
  }

  public String getExternalReference() {
    return externalReference;
  }

  public LocalDate getPublishedOn() {
    return publishedOn;
  }

  public LocalDate getSubmissionDeadline() {
    return submissionDeadline;
  }

  public LocalDate getDecisionOn() {
    return decisionOn;
  }

  public String getNotes() {
    return notes;
  }

  public Long getContractId() {
    return contractId;
  }

  public LocalDateTime getSubmittedAt() {
    return submittedAt;
  }

  public LocalDateTime getClosedAt() {
    return closedAt;
  }

  public void applyDetails(
      Long organizationId,
      String title,
      String sourceUrl,
      String externalReference,
      LocalDate publishedOn,
      LocalDate submissionDeadline,
      LocalDate decisionOn,
      String notes) {
    this.organizationId = organizationId;
    this.title = title != null ? title.strip() : "";
    this.sourceUrl = Strings.blankToNull(sourceUrl);
    this.externalReference = Strings.blankToNull(externalReference);
    this.publishedOn = publishedOn;
    this.submissionDeadline = submissionDeadline;
    this.decisionOn = decisionOn;
    this.notes = Strings.blankToNull(notes);
    touch();
  }

  public void transitionTo(OpportunityStatus next) {
    this.status = next;
    touch();
  }

  public void markSubmittedNow() {
    this.submittedAt = LocalDateTime.now();
    touch();
  }

  public void markClosedNow() {
    this.closedAt = LocalDateTime.now();
    touch();
  }

  public void linkContract(Long contractId) {
    this.contractId = contractId;
    touch();
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    touch();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private Long id;
    private Long organizationId;
    private String title;
    private OpportunityStatus status;
    private String sourceUrl;
    private String externalReference;
    private LocalDate publishedOn;
    private LocalDate submissionDeadline;
    private LocalDate decisionOn;
    private String notes;
    private Long contractId;
    private LocalDateTime submittedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withOrganizationId(Long organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public SafeBuilder withTitle(String title) {
      this.title = title;
      return this;
    }

    public SafeBuilder withStatus(OpportunityStatus status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withSourceUrl(String sourceUrl) {
      this.sourceUrl = sourceUrl;
      return this;
    }

    public SafeBuilder withExternalReference(String externalReference) {
      this.externalReference = externalReference;
      return this;
    }

    public SafeBuilder withPublishedOn(LocalDate publishedOn) {
      this.publishedOn = publishedOn;
      return this;
    }

    public SafeBuilder withSubmissionDeadline(LocalDate submissionDeadline) {
      this.submissionDeadline = submissionDeadline;
      return this;
    }

    public SafeBuilder withDecisionOn(LocalDate decisionOn) {
      this.decisionOn = decisionOn;
      return this;
    }

    public SafeBuilder withNotes(String notes) {
      this.notes = notes;
      return this;
    }

    public SafeBuilder withContractId(Long contractId) {
      this.contractId = contractId;
      return this;
    }

    public SafeBuilder withSubmittedAt(LocalDateTime submittedAt) {
      this.submittedAt = submittedAt;
      return this;
    }

    public SafeBuilder withClosedAt(LocalDateTime closedAt) {
      this.closedAt = closedAt;
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

    public Opportunity register() {
      Opportunity o = new Opportunity();
      o.organizationId = organizationId;
      o.title = title != null ? title.strip() : "";
      o.status = OpportunityStatus.DRAFT;
      o.sourceUrl = Strings.blankToNull(sourceUrl);
      o.externalReference = Strings.blankToNull(externalReference);
      o.publishedOn = publishedOn;
      o.submissionDeadline = submissionDeadline;
      o.decisionOn = decisionOn;
      o.notes = Strings.blankToNull(notes);
      o.createdAt = LocalDateTime.now();
      o.updatedAt = o.createdAt;
      return o;
    }

    public Opportunity reconstruct() {
      Opportunity o = new Opportunity();
      o.id = id != null ? id : 0L;
      o.organizationId = organizationId;
      o.title = title;
      o.status = status != null ? status : OpportunityStatus.UNDEFINED;
      o.sourceUrl = sourceUrl;
      o.externalReference = externalReference;
      o.publishedOn = publishedOn;
      o.submissionDeadline = submissionDeadline;
      o.decisionOn = decisionOn;
      o.notes = notes;
      o.contractId = contractId;
      o.submittedAt = submittedAt;
      o.closedAt = closedAt;
      o.createdAt = createdAt;
      o.updatedAt = updatedAt;
      o.deletedAt = deletedAt;
      o.version = version;
      return o;
    }
  }
}
