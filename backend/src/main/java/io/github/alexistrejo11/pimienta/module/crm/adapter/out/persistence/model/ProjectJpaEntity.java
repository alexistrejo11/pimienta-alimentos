package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "crm_projects",
    indexes = {
      @Index(name = "idx_crm_projects_status", columnList = "status"),
      @Index(name = "idx_crm_projects_client_id", columnList = "client_id"),
      @Index(name = "idx_crm_projects_origin_opportunity_id", columnList = "origin_opportunity_id"),
      @Index(name = "idx_crm_projects_deleted_at", columnList = "deleted_at")
    })
public class ProjectJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "client_id")
  private Long clientId;

  @Column(name = "origin_opportunity_id")
  private Long originOpportunityId;

  @Column(name = "project_code", nullable = false, unique = true, length = 64)
  private String projectCode;

  @Column(name = "project_name", nullable = false, length = 500)
  private String projectName;

  @Column(length = 4000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Project.ProjectType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Project.ProjectStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Project.ProjectPriority priority;

  @Column(name = "project_manager_id")
  private Long projectManagerId;

  @Column(name = "assigned_salesman_id")
  private Long assignedSalesmanId;

  @Column(name = "planned_start_date")
  private LocalDate plannedStartDate;

  @Column(name = "planned_end_date")
  private LocalDate plannedEndDate;

  @Column(name = "actual_start_date")
  private LocalDate actualStartDate;

  @Column(name = "actual_end_date")
  private LocalDate actualEndDate;

  @Column(name = "on_hold_reason", length = 2000)
  private String onHoldReason;

  @Column(name = "contracted_value", precision = 19, scale = 4)
  private BigDecimal contractedValue;

  @Column(name = "estimated_cost", precision = 19, scale = 4)
  private BigDecimal estimatedCost;

  @Column(name = "actual_cost", precision = 19, scale = 4)
  private BigDecimal actualCost;

  @Column(name = "progress_percent", nullable = false)
  private int progressPercent;

  @Column(name = "cancellation_reason", length = 2000)
  private String cancellationReason;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public void setOriginOpportunityId(Long originOpportunityId) {
    this.originOpportunityId = originOpportunityId;
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

  public Project.ProjectType getType() {
    return type;
  }

  public void setType(Project.ProjectType type) {
    this.type = type;
  }

  public Project.ProjectStatus getStatus() {
    return status;
  }

  public void setStatus(Project.ProjectStatus status) {
    this.status = status;
  }

  public Project.ProjectPriority getPriority() {
    return priority;
  }

  public void setPriority(Project.ProjectPriority priority) {
    this.priority = priority;
  }

  public Long getProjectManagerId() {
    return projectManagerId;
  }

  public void setProjectManagerId(Long projectManagerId) {
    this.projectManagerId = projectManagerId;
  }

  public Long getAssignedSalesmanId() {
    return assignedSalesmanId;
  }

  public void setAssignedSalesmanId(Long assignedSalesmanId) {
    this.assignedSalesmanId = assignedSalesmanId;
  }

  public LocalDate getPlannedStartDate() {
    return plannedStartDate;
  }

  public void setPlannedStartDate(LocalDate plannedStartDate) {
    this.plannedStartDate = plannedStartDate;
  }

  public LocalDate getPlannedEndDate() {
    return plannedEndDate;
  }

  public void setPlannedEndDate(LocalDate plannedEndDate) {
    this.plannedEndDate = plannedEndDate;
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

  public BigDecimal getContractedValue() {
    return contractedValue;
  }

  public void setContractedValue(BigDecimal contractedValue) {
    this.contractedValue = contractedValue;
  }

  public BigDecimal getEstimatedCost() {
    return estimatedCost;
  }

  public void setEstimatedCost(BigDecimal estimatedCost) {
    this.estimatedCost = estimatedCost;
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

  public String getCancellationReason() {
    return cancellationReason;
  }

  public void setCancellationReason(String cancellationReason) {
    this.cancellationReason = cancellationReason;
  }
}
