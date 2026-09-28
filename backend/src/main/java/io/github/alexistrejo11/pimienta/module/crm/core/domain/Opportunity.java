package io.github.alexistrejo11.pimienta.module.crm.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sales opportunity aggregate: persistence-shaped, no business rules. Use cases orchestrate state
 * changes; format validation stays on Jakarta-annotated web DTOs. Build with {@link #builder()} and
 * {@link SafeBuilder#register()} or {@link SafeBuilder#reconstruct()}.
 */
public class Opportunity extends BaseDomain<Long> {

  public enum OpportunityStatus {
    NEW,
    DISCOVERY,
    PROPOSAL,
    NEGOTIATION,
    WON,
    LOST,
    ABANDONED
  }

  public enum OpportunitySource {
    INBOUND,
    OUTBOUND,
    REFERRAL,
    SOCIAL_MEDIA,
    EVENT,
    COLD_CALL,
    OTHER
  }

  private String contactName;
  private String contactEmail;
  private String contactPhone;
  private String companyName;
  private String companyLocation;
  private String industry;
  private String title;
  private String description;
  private BigDecimal estimatedValue;
  private int probabilityPercent;
  private OpportunitySource source;
  private OpportunityStatus status;
  private LocalDate expectedCloseDate;
  private LocalDate actualCloseDate;
  private Long assignedSalesmanId;
  private String lostReason;
  private Long convertedProjectId;

  private Opportunity() {
    this.id = 0L;
    this.probabilityPercent = 10;
    this.estimatedValue = BigDecimal.ZERO;
    this.status = OpportunityStatus.NEW;
    this.source = OpportunitySource.OTHER;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  /** Bumps {@code updatedAt} only (same as former transition methods). */
  public void touch() {
    setUpdatedAt(LocalDateTime.now());
  }

  public void softDelete() {
    setDeletedAt(LocalDateTime.now());
    setUpdatedAt(LocalDateTime.now());
  }

  /** Weighted value by probability (read model). */
  public BigDecimal getWeightedValue() {
    BigDecimal ev = estimatedValue != null ? estimatedValue : BigDecimal.ZERO;
    return ev.multiply(
        new BigDecimal(probabilityPercent)
            .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
  }

  public boolean isOverdue() {
    return expectedCloseDate != null
        && LocalDate.now().isAfter(expectedCloseDate)
        && (status != OpportunityStatus.WON && status != OpportunityStatus.LOST);
  }

  public boolean isActive() {
    return status != OpportunityStatus.WON
        && status != OpportunityStatus.LOST
        && status != OpportunityStatus.ABANDONED;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public void setContactPhone(String contactPhone) {
    this.contactPhone = contactPhone;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getCompanyLocation() {
    return companyLocation;
  }

  public void setCompanyLocation(String companyLocation) {
    this.companyLocation = companyLocation;
  }

  public String getIndustry() {
    return industry;
  }

  public void setIndustry(String industry) {
    this.industry = industry;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getEstimatedValue() {
    return estimatedValue;
  }

  public void setEstimatedValue(BigDecimal estimatedValue) {
    this.estimatedValue = estimatedValue;
  }

  public int getProbabilityPercent() {
    return probabilityPercent;
  }

  public void setProbabilityPercent(int probabilityPercent) {
    this.probabilityPercent = probabilityPercent;
  }

  public OpportunitySource getSource() {
    return source;
  }

  public void setSource(OpportunitySource source) {
    this.source = source;
  }

  public OpportunityStatus getStatus() {
    return status;
  }

  public void setStatus(OpportunityStatus status) {
    this.status = status;
  }

  public LocalDate getExpectedCloseDate() {
    return expectedCloseDate;
  }

  public void setExpectedCloseDate(LocalDate expectedCloseDate) {
    this.expectedCloseDate = expectedCloseDate;
  }

  public LocalDate getActualCloseDate() {
    return actualCloseDate;
  }

  public void setActualCloseDate(LocalDate actualCloseDate) {
    this.actualCloseDate = actualCloseDate;
  }

  public Long getAssignedSalesmanId() {
    return assignedSalesmanId;
  }

  public void setAssignedSalesmanId(Long assignedSalesmanId) {
    this.assignedSalesmanId = assignedSalesmanId;
  }

  public String getLostReason() {
    return lostReason;
  }

  public void setLostReason(String lostReason) {
    this.lostReason = lostReason;
  }

  public Long getConvertedProjectId() {
    return convertedProjectId;
  }

  public void setConvertedProjectId(Long convertedProjectId) {
    this.convertedProjectId = convertedProjectId;
  }

  public static final class SafeBuilder {
    private Long id;
    private String contactName;
    private String contactEmail;
    private String contactPhone;
    private String companyName;
    private String companyLocation;
    private String industry;
    private String title;
    private String description;
    private BigDecimal estimatedValue;
    private int probabilityPercent;
    private OpportunitySource source;
    private OpportunityStatus status;
    private LocalDate expectedCloseDate;
    private LocalDate actualCloseDate;
    private Long assignedSalesmanId;
    private String lostReason;
    private Long convertedProjectId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withContactName(String contactName) {
      this.contactName = contactName;
      return this;
    }

    public SafeBuilder withContactEmail(String contactEmail) {
      this.contactEmail = contactEmail;
      return this;
    }

    public SafeBuilder withContactPhone(String contactPhone) {
      this.contactPhone = contactPhone;
      return this;
    }

    public SafeBuilder withCompanyName(String companyName) {
      this.companyName = companyName;
      return this;
    }

    public SafeBuilder withCompanyLocation(String companyLocation) {
      this.companyLocation = companyLocation;
      return this;
    }

    public SafeBuilder withIndustry(String industry) {
      this.industry = industry;
      return this;
    }

    public SafeBuilder withTitle(String title) {
      this.title = title;
      return this;
    }

    public SafeBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public SafeBuilder withEstimatedValue(BigDecimal estimatedValue) {
      this.estimatedValue = estimatedValue;
      return this;
    }

    public SafeBuilder withProbabilityPercent(int probabilityPercent) {
      this.probabilityPercent = probabilityPercent;
      return this;
    }

    public SafeBuilder withSource(OpportunitySource source) {
      this.source = source;
      return this;
    }

    public SafeBuilder withStatus(OpportunityStatus status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withExpectedCloseDate(LocalDate expectedCloseDate) {
      this.expectedCloseDate = expectedCloseDate;
      return this;
    }

    public SafeBuilder withActualCloseDate(LocalDate actualCloseDate) {
      this.actualCloseDate = actualCloseDate;
      return this;
    }

    public SafeBuilder withAssignedSalesmanId(Long assignedSalesmanId) {
      this.assignedSalesmanId = assignedSalesmanId;
      return this;
    }

    public SafeBuilder withLostReason(String lostReason) {
      this.lostReason = lostReason;
      return this;
    }

    public SafeBuilder withConvertedProjectId(Long convertedProjectId) {
      this.convertedProjectId = convertedProjectId;
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

    public Opportunity reconstruct() {
      Opportunity o = new Opportunity();
      o.setId(id != null ? id : 0L);
      o.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
      o.setUpdatedAt(updatedAt != null ? updatedAt : o.getCreatedAt());
      o.setDeletedAt(deletedAt);
      o.setVersion(version);
      o.contactName = contactName;
      o.contactEmail = contactEmail;
      o.contactPhone = contactPhone;
      o.companyName = companyName;
      o.companyLocation = companyLocation;
      o.industry = industry;
      o.title = title;
      o.description = description;
      o.estimatedValue = estimatedValue != null ? estimatedValue : BigDecimal.ZERO;
      o.probabilityPercent = probabilityPercent;
      o.source = source != null ? source : OpportunitySource.OTHER;
      o.status = status != null ? status : OpportunityStatus.NEW;
      o.expectedCloseDate = expectedCloseDate;
      o.actualCloseDate = actualCloseDate;
      o.assignedSalesmanId = assignedSalesmanId;
      o.lostReason = lostReason;
      o.convertedProjectId = convertedProjectId;
      return o;
    }

    /** New opportunity row; timestamps and defaults applied here. */
    public Opportunity register() {
      var now = LocalDateTime.now();
      Opportunity o = new Opportunity();
      o.setId(0L);
      o.title = title;
      o.description = description;
      o.contactName = contactName;
      o.contactEmail = contactEmail;
      o.contactPhone = contactPhone;
      o.companyName = companyName;
      o.companyLocation = companyLocation;
      o.industry = industry;
      o.estimatedValue = estimatedValue != null ? estimatedValue : BigDecimal.ZERO;
      o.probabilityPercent = probabilityPercent;
      o.source = source != null ? source : OpportunitySource.OTHER;
      o.status = OpportunityStatus.NEW;
      o.expectedCloseDate = expectedCloseDate;
      o.assignedSalesmanId = assignedSalesmanId;
      o.actualCloseDate = null;
      o.lostReason = null;
      o.convertedProjectId = null;
      o.setCreatedAt(now);
      o.setUpdatedAt(now);
      o.setDeletedAt(null);
      o.setVersion(version);
      return o;
    }
  }
}
