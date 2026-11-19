package io.github.alexistrejo11.pimienta.module.opportunity.core.domain;

import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementCategory;
import io.github.alexistrejo11.pimienta.module.opportunity.core.domain.enums.RequirementStatus;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import io.github.alexistrejo11.pimienta.shared.util.Strings;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OpportunityRequirement extends BaseDomain<Long> {

  private Long opportunityId;
  private String title;
  private RequirementCategory category;
  private boolean mandatory;
  private RequirementStatus status;
  private LocalDate dueOn;
  private String notes;
  private int sortOrder;

  private OpportunityRequirement() {
    this.id = 0L;
    this.opportunityId = 0L;
    this.title = "";
    this.category = RequirementCategory.OTHER;
    this.mandatory = true;
    this.status = RequirementStatus.PENDING;
    this.sortOrder = 0;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public Long getOpportunityId() {
    return opportunityId != null ? opportunityId : 0L;
  }

  public String getTitle() {
    return title != null ? title : "";
  }

  public RequirementCategory getCategory() {
    return category != null ? category : RequirementCategory.UNDEFINED;
  }

  public boolean isMandatory() {
    return mandatory;
  }

  public RequirementStatus getStatus() {
    return status != null ? status : RequirementStatus.UNDEFINED;
  }

  public LocalDate getDueOn() {
    return dueOn;
  }

  public String getNotes() {
    return notes;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void applyDetails(
      String title,
      RequirementCategory category,
      boolean mandatory,
      LocalDate dueOn,
      String notes,
      int sortOrder) {
    this.title = title != null ? title.strip() : "";
    this.category = category != null ? category : RequirementCategory.OTHER;
    this.mandatory = mandatory;
    this.dueOn = dueOn;
    this.notes = Strings.blankToNull(notes);
    this.sortOrder = sortOrder;
    touch();
  }

  public void changeStatus(RequirementStatus status) {
    this.status = status != null ? status : RequirementStatus.PENDING;
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
    private Long opportunityId;
    private String title;
    private RequirementCategory category;
    private boolean mandatory = true;
    private RequirementStatus status;
    private LocalDate dueOn;
    private String notes;
    private int sortOrder;
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

    public SafeBuilder withTitle(String title) {
      this.title = title;
      return this;
    }

    public SafeBuilder withCategory(RequirementCategory category) {
      this.category = category;
      return this;
    }

    public SafeBuilder withMandatory(boolean mandatory) {
      this.mandatory = mandatory;
      return this;
    }

    public SafeBuilder withStatus(RequirementStatus status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withDueOn(LocalDate dueOn) {
      this.dueOn = dueOn;
      return this;
    }

    public SafeBuilder withNotes(String notes) {
      this.notes = notes;
      return this;
    }

    public SafeBuilder withSortOrder(int sortOrder) {
      this.sortOrder = sortOrder;
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

    public OpportunityRequirement register() {
      OpportunityRequirement r = new OpportunityRequirement();
      r.opportunityId = opportunityId;
      r.title = title != null ? title.strip() : "";
      r.category = category != null ? category : RequirementCategory.OTHER;
      r.mandatory = mandatory;
      r.status = RequirementStatus.PENDING;
      r.dueOn = dueOn;
      r.notes = Strings.blankToNull(notes);
      r.sortOrder = sortOrder;
      r.createdAt = LocalDateTime.now();
      r.updatedAt = r.createdAt;
      return r;
    }

    public OpportunityRequirement reconstruct() {
      OpportunityRequirement r = new OpportunityRequirement();
      r.id = id != null ? id : 0L;
      r.opportunityId = opportunityId;
      r.title = title;
      r.category = category != null ? category : RequirementCategory.UNDEFINED;
      r.mandatory = mandatory;
      r.status = status != null ? status : RequirementStatus.UNDEFINED;
      r.dueOn = dueOn;
      r.notes = notes;
      r.sortOrder = sortOrder;
      r.createdAt = createdAt;
      r.updatedAt = updatedAt;
      r.deletedAt = deletedAt;
      r.version = version;
      return r;
    }
  }
}
