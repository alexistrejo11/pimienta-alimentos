package io.github.alexistrejo11.pimienta.module.crm.adapter.out.persistence.model;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.ProjectMilestone;
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
    name = "crm_project_milestones",
    indexes = {
      @Index(name = "idx_crm_project_milestones_project_sort", columnList = "project_id, sort_order"),
      @Index(name = "idx_crm_project_milestones_deleted_at", columnList = "deleted_at")
    })
public class ProjectMilestoneJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(nullable = false, length = 500)
  private String name;

  @Column(length = 4000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private ProjectMilestone.MilestoneStatus status;

  @Column(name = "planned_date")
  private LocalDate plannedDate;

  @Column(name = "actual_date")
  private LocalDate actualDate;

  @Column(name = "billing_amount", precision = 19, scale = 4)
  private BigDecimal billingAmount;

  @Column(nullable = false)
  private boolean billed;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public ProjectMilestone.MilestoneStatus getStatus() {
    return status;
  }

  public void setStatus(ProjectMilestone.MilestoneStatus status) {
    this.status = status;
  }

  public LocalDate getPlannedDate() {
    return plannedDate;
  }

  public void setPlannedDate(LocalDate plannedDate) {
    this.plannedDate = plannedDate;
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

  public void setBillingAmount(BigDecimal billingAmount) {
    this.billingAmount = billingAmount;
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
}
