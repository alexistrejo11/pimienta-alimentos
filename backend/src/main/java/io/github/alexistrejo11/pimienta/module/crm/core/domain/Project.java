package io.github.alexistrejo11.pimienta.module.crm.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Project aggregate: persistence-shaped, no business rules. Use cases orchestrate lifecycle;
 * format validation on web DTOs. Build with {@link #builder()} and {@link SafeBuilder#register()}
 * or {@link SafeBuilder#reconstruct()}.
 */
public class Project extends BaseDomain<Long> {

  public enum ProjectStatus {
    PLANNING,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    CANCELLED,
    ARCHIVED
  }

  public enum ProjectType {
    CONSULTING,
    SOFTWARE_DEVELOPMENT,
    IMPLEMENTATION,
    MAINTENANCE,
    TRAINING,
    RESEARCH,
    OTHER
  }

  public enum ProjectPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  private Long clientId;
  private Long originOpportunityId;
  private String projectCode;
  private String projectName;
  private String description;
  private ProjectType type;
  private ProjectStatus status;
  private ProjectPriority priority;
  private Long projectManagerId;
  private Long assignedSalesmanId;
  private LocalDate plannedStartDate;
  private LocalDate plannedEndDate;
  private LocalDate actualStartDate;
  private LocalDate actualEndDate;
  private String onHoldReason;
  private BigDecimal contractedValue;
  private BigDecimal estimatedCost;
  private BigDecimal actualCost;
  private int progressPercent;
  private String cancellationReason;

  private Project() {
    this.id = 0L;
    this.progressPercent = 0;
    this.contractedValue = BigDecimal.ZERO;
    this.estimatedCost = BigDecimal.ZERO;
    this.actualCost = BigDecimal.ZERO;
    this.status = ProjectStatus.PLANNING;
    this.priority = ProjectPriority.MEDIUM;
    this.type = ProjectType.OTHER;
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

  public BigDecimal getGrossMargin() {
    BigDecimal contracted = contractedValue != null ? contractedValue : BigDecimal.ZERO;
    BigDecimal actual = actualCost != null ? actualCost : BigDecimal.ZERO;
    return contracted.subtract(actual);
  }

  public BigDecimal getMarginPercent() {
    BigDecimal contracted = contractedValue != null ? contractedValue : BigDecimal.ZERO;
    if (contracted.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return getGrossMargin()
        .divide(contracted, 4, RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
  }

  public boolean isOverBudget() {
    BigDecimal est = estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
    BigDecimal act = actualCost != null ? actualCost : BigDecimal.ZERO;
    return act.compareTo(est) > 0;
  }

  public boolean isOverdue() {
    return status == ProjectStatus.ACTIVE
        && plannedEndDate != null
        && LocalDate.now().isAfter(plannedEndDate);
  }

  public boolean isActive() {
    return status == ProjectStatus.ACTIVE || status == ProjectStatus.ON_HOLD;
  }

  public Long getClientId() {
    return clientId;
  }

  public void setClientId(Long clientId) {
    this.clientId = clientId;
  }

  public Long getOriginOpportunityId() {
    return originOpportunityId;
  }

  public void setOriginOpportunityId(Long id) {
    this.originOpportunityId = id;
  }

  public String getProjectCode() {
    return projectCode;
  }

  public void setProjectCode(String projectCode) {
    this.projectCode = projectCode;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ProjectType getType() {
    return type;
  }

  public void setType(ProjectType type) {
    this.type = type;
  }

  public ProjectStatus getStatus() {
    return status;
  }

  public void setStatus(ProjectStatus status) {
    this.status = status;
  }

  public ProjectPriority getPriority() {
    return priority;
  }

  public void setPriority(ProjectPriority priority) {
    this.priority = priority;
  }

  public Long getProjectManagerId() {
    return projectManagerId;
  }

  public void setProjectManagerId(Long id) {
    this.projectManagerId = id;
  }

  public Long getAssignedSalesmanId() {
    return assignedSalesmanId;
  }

  public void setAssignedSalesmanId(Long id) {
    this.assignedSalesmanId = id;
  }

  public LocalDate getPlannedStartDate() {
    return plannedStartDate;
  }

  public void setPlannedStartDate(LocalDate d) {
    this.plannedStartDate = d;
  }

  public LocalDate getPlannedEndDate() {
    return plannedEndDate;
  }

  public void setPlannedEndDate(LocalDate d) {
    this.plannedEndDate = d;
  }

  public LocalDate getActualStartDate() {
    return actualStartDate;
  }

  public void setActualStartDate(LocalDate actualStartDate) {
    this.actualStartDate = actualStartDate;
  }

  public LocalDate getActualEndDate() {
    return actualEndDate;
  }

  public void setActualEndDate(LocalDate actualEndDate) {
    this.actualEndDate = actualEndDate;
  }

  public String getOnHoldReason() {
    return onHoldReason;
  }

  public void setOnHoldReason(String onHoldReason) {
    this.onHoldReason = onHoldReason;
  }

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }

  public BigDecimal getContractedValue() {
    return contractedValue;
  }

  public void setContractedValue(BigDecimal v) {
    this.contractedValue = v;
  }

  public BigDecimal getEstimatedCost() {
    return estimatedCost;
  }

  public void setEstimatedCost(BigDecimal v) {
    this.estimatedCost = v;
  }

  public BigDecimal getActualCost() {
    return actualCost;
  }

  public void setActualCost(BigDecimal actualCost) {
    this.actualCost = actualCost;
  }

  public int getProgressPercent() {
    return progressPercent;
  }

  public void setProgressPercent(int progressPercent) {
    this.progressPercent = progressPercent;
  }

  public static final class SafeBuilder {
    private Long id;
    private Long clientId;
    private Long originOpportunityId;
    private String projectCode;
    private String projectName;
    private String description;
    private ProjectType type;
    private ProjectStatus status;
    private ProjectPriority priority;
    private Long projectManagerId;
    private Long assignedSalesmanId;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private String onHoldReason;
    private BigDecimal contractedValue;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private int progressPercent;
    private String cancellationReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withClientId(Long clientId) {
      this.clientId = clientId;
      return this;
    }

    public SafeBuilder withOriginOpportunityId(Long originOpportunityId) {
      this.originOpportunityId = originOpportunityId;
      return this;
    }

    public SafeBuilder withProjectCode(String projectCode) {
      this.projectCode = projectCode;
      return this;
    }

    public SafeBuilder withProjectName(String projectName) {
      this.projectName = projectName;
      return this;
    }

    public SafeBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public SafeBuilder withType(ProjectType type) {
      this.type = type;
      return this;
    }

    public SafeBuilder withStatus(ProjectStatus status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withPriority(ProjectPriority priority) {
      this.priority = priority;
      return this;
    }

    public SafeBuilder withProjectManagerId(Long projectManagerId) {
      this.projectManagerId = projectManagerId;
      return this;
    }

    public SafeBuilder withAssignedSalesmanId(Long assignedSalesmanId) {
      this.assignedSalesmanId = assignedSalesmanId;
      return this;
    }

    public SafeBuilder withPlannedStartDate(LocalDate plannedStartDate) {
      this.plannedStartDate = plannedStartDate;
      return this;
    }

    public SafeBuilder withPlannedEndDate(LocalDate plannedEndDate) {
      this.plannedEndDate = plannedEndDate;
      return this;
    }

    public SafeBuilder withActualStartDate(LocalDate actualStartDate) {
      this.actualStartDate = actualStartDate;
      return this;
    }

    public SafeBuilder withActualEndDate(LocalDate actualEndDate) {
      this.actualEndDate = actualEndDate;
      return this;
    }

    public SafeBuilder withOnHoldReason(String onHoldReason) {
      this.onHoldReason = onHoldReason;
      return this;
    }

    public SafeBuilder withContractedValue(BigDecimal contractedValue) {
      this.contractedValue = contractedValue;
      return this;
    }

    public SafeBuilder withEstimatedCost(BigDecimal estimatedCost) {
      this.estimatedCost = estimatedCost;
      return this;
    }

    public SafeBuilder withActualCost(BigDecimal actualCost) {
      this.actualCost = actualCost;
      return this;
    }

    public SafeBuilder withProgressPercent(int progressPercent) {
      this.progressPercent = progressPercent;
      return this;
    }

    public SafeBuilder withCancellationReason(String cancellationReason) {
      this.cancellationReason = cancellationReason;
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

    public Project reconstruct() {
      Project project = new Project();
      project.setId(id != null ? id : 0L);
      project.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());
      project.setUpdatedAt(updatedAt != null ? updatedAt : project.getCreatedAt());
      project.setDeletedAt(deletedAt);
      project.setVersion(version);
      project.clientId = clientId;
      project.originOpportunityId = originOpportunityId;
      project.projectCode = projectCode;
      project.projectName = projectName;
      project.description = description;
      project.type = type != null ? type : ProjectType.OTHER;
      project.status = status != null ? status : ProjectStatus.PLANNING;
      project.priority = priority != null ? priority : ProjectPriority.MEDIUM;
      project.projectManagerId = projectManagerId;
      project.assignedSalesmanId = assignedSalesmanId;
      project.plannedStartDate = plannedStartDate;
      project.plannedEndDate = plannedEndDate;
      project.actualStartDate = actualStartDate;
      project.actualEndDate = actualEndDate;
      project.onHoldReason = onHoldReason;
      project.contractedValue = contractedValue != null ? contractedValue : BigDecimal.ZERO;
      project.estimatedCost = estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
      project.actualCost = actualCost != null ? actualCost : BigDecimal.ZERO;
      project.progressPercent = progressPercent;
      project.cancellationReason = cancellationReason;
      return project;
    }

    public Project register() {
      var now = LocalDateTime.now();
      Project project = new Project();
      project.setId(0L);
      project.projectCode = projectCode;
      project.projectName = projectName;
      project.description = description;
      project.clientId = clientId;
      project.originOpportunityId = originOpportunityId;
      project.type = type != null ? type : ProjectType.OTHER;
      project.priority = priority != null ? priority : ProjectPriority.MEDIUM;
      project.projectManagerId = projectManagerId;
      project.assignedSalesmanId = assignedSalesmanId;
      project.plannedStartDate = plannedStartDate;
      project.plannedEndDate = plannedEndDate;
      project.contractedValue = contractedValue != null ? contractedValue : BigDecimal.ZERO;
      project.estimatedCost = estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
      project.actualCost = BigDecimal.ZERO;
      project.status = ProjectStatus.PLANNING;
      project.progressPercent = 0;
      project.setCreatedAt(now);
      project.setUpdatedAt(now);
      project.setDeletedAt(null);
      project.setVersion(version);
      return project;
    }
  }
}
