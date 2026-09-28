package io.github.alexistrejo11.pimienta.module.crm.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Project milestone aggregate: persistence-shaped, no business rules. Use cases orchestrate status
 * and billing flags. Build with {@link #builder()} and {@link SafeBuilder#register()} or {@link
 * SafeBuilder#reconstruct()}.
 */
public class ProjectMilestone extends BaseDomain<Long> {

  public enum MilestoneStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    DELAYED,
    CANCELLED
  }

  private Long projectId;
  private String name;
  private String description;
  private MilestoneStatus status;
  private LocalDate plannedDate;
  private LocalDate actualDate;
  private BigDecimal billingAmount;
  private boolean billed;
  private int sortOrder;

  private ProjectMilestone() {
    this.id = 0L;
    this.status = MilestoneStatus.PENDING;
    this.billingAmount = BigDecimal.ZERO;
    this.billed = false;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public void touch() {
    setUpdatedAt(LocalDateTime.now());
  }

  public void softDelete() {
    setDeletedAt(LocalDateTime.now());
    setUpdatedAt(LocalDateTime.now());
  }

  public boolean isOverdue() {
    return status != MilestoneStatus.COMPLETED
        && status != MilestoneStatus.CANCELLED
        && plannedDate != null
        && LocalDate.now().isAfter(plannedDate);
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public MilestoneStatus getStatus() {
    return status;
  }

  public void setStatus(MilestoneStatus status) {
    this.status = status;
  }

  public LocalDate getPlannedDate() {
    return plannedDate;
  }

  public void setPlannedDate(LocalDate d) {
    this.plannedDate = d;
  }

  public LocalDate getActualDate() {
    return actualDate;
  }

  public void setActualDate(LocalDate actualDate) {
    this.actualDate = actualDate;
  }

  public BigDecimal getBillingAmount() {
    return billingAmount;
  }

  public void setBillingAmount(BigDecimal v) {
    this.billingAmount = v;
  }

  public boolean isBilled() {
    return billed;
  }

  public void setBilled(boolean billed) {
    this.billed = billed;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public static final class SafeBuilder {
    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private MilestoneStatus status;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private BigDecimal billingAmount;
    private boolean billed;
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withProjectId(Long projectId) {
      this.projectId = projectId;
      return this;
    }

    public SafeBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public SafeBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public SafeBuilder withStatus(MilestoneStatus status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withPlannedDate(LocalDate plannedDate) {
      this.plannedDate = plannedDate;
      return this;
    }

    public SafeBuilder withActualDate(LocalDate actualDate) {
      this.actualDate = actualDate;
      return this;
    }

    public SafeBuilder withBillingAmount(BigDecimal billingAmount) {
      this.billingAmount = billingAmount;
      return this;
    }

    public SafeBuilder withBilled(boolean billed) {
      this.billed = billed;
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

    public ProjectMilestone reconstruct() {
      ProjectMilestone m = new ProjectMilestone();
      m.setId(id != null ? id : 0L);
      m.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
      m.setUpdatedAt(updatedAt != null ? updatedAt : m.getCreatedAt());
      m.setDeletedAt(deletedAt);
      m.setVersion(version);
      m.projectId = projectId;
      m.name = name;
      m.description = description;
      m.status = status != null ? status : MilestoneStatus.PENDING;
      m.plannedDate = plannedDate;
      m.actualDate = actualDate;
      m.billingAmount = billingAmount != null ? billingAmount : BigDecimal.ZERO;
      m.billed = billed;
      m.sortOrder = sortOrder;
      return m;
    }

    public ProjectMilestone register() {
      var now = LocalDateTime.now();
      ProjectMilestone m = new ProjectMilestone();
      m.setId(0L);
      m.projectId = projectId;
      m.name = name;
      m.description = description;
      m.plannedDate = plannedDate;
      m.billingAmount = billingAmount != null ? billingAmount : BigDecimal.ZERO;
      m.sortOrder = sortOrder;
      m.status = MilestoneStatus.PENDING;
      m.billed = false;
      m.setCreatedAt(now);
      m.setUpdatedAt(now);
      m.setDeletedAt(null);
      m.setVersion(version);
      return m;
    }
  }
}
