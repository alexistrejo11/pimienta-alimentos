package io.github.alexistrejo11.pimienta.module.opportunity.core.domain;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.ProposalKind;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;

public class OpportunityProposal extends BaseDomain<Long> {

  private Long opportunityId;
  private ProposalKind kind;
  private String notes;

  private OpportunityProposal() {
    this.id = 0L;
    this.opportunityId = 0L;
    this.kind = ProposalKind.COMBINED;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public Long getOpportunityId() {
    return opportunityId != null ? opportunityId : 0L;
  }

  public ProposalKind getKind() {
    return kind != null ? kind : ProposalKind.UNDEFINED;
  }

  public String getNotes() {
    return notes;
  }

  public void applyDetails(ProposalKind kind, String notes) {
    this.kind = kind != null ? kind : ProposalKind.COMBINED;
    this.notes = blankToNull(notes);
    touch();
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    touch();
  }

  private static String blankToNull(String value) {
    if (value == null) {
      return null;
    }
    String t = value.strip();
    return t.isEmpty() ? null : t;
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private Long id;
    private Long opportunityId;
    private ProposalKind kind;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withOpportunityId(Long opportunityId) {
      this.opportunityId = opportunityId;
      return this;
    }

    public SafeBuilder withKind(ProposalKind kind) {
      this.kind = kind;
      return this;
    }

    public SafeBuilder withNotes(String notes) {
      this.notes = notes;
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

    public OpportunityProposal register() {
      OpportunityProposal p = new OpportunityProposal();
      p.opportunityId = opportunityId;
      p.kind = kind != null ? kind : ProposalKind.COMBINED;
      p.notes = blankToNull(notes);
      p.createdAt = LocalDateTime.now();
      p.updatedAt = p.createdAt;
      return p;
    }

    public OpportunityProposal reconstruct() {
      OpportunityProposal p = new OpportunityProposal();
      p.id = id != null ? id : 0L;
      p.opportunityId = opportunityId;
      p.kind = kind != null ? kind : ProposalKind.UNDEFINED;
      p.notes = notes;
      p.createdAt = createdAt;
      p.updatedAt = updatedAt;
      p.deletedAt = deletedAt;
      p.version = version;
      return p;
    }
  }
}
