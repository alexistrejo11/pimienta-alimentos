package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.out.persistence;

import io.github.alexistrejo11.pimienta.shared.jpa.BaseJpaEntity;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "tasks",
    indexes = {
      @Index(name = "idx_tasks_assigned_to_id", columnList = "assigned_to_id"),
      @Index(name = "idx_tasks_status", columnList = "status"),
      @Index(name = "idx_tasks_project_id", columnList = "project_id"),
      @Index(name = "idx_tasks_opportunity_id", columnList = "opportunity_id"),
      @Index(name = "idx_tasks_headquarter_id", columnList = "headquarter_id"),
      @Index(name = "idx_tasks_due_date", columnList = "due_date"),
      @Index(name = "idx_tasks_deleted_at", columnList = "deleted_at")
    })
public class TaskJpaEntity extends BaseJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 300)
  private String title;

  @Column(length = 4000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private Task.Status status;

  @Enumerated(EnumType.STRING)
  @Column(length = 32)
  private Task.Priority priority;

  @Column(name = "assigned_to_id")
  private Long assignedToId;

  @Column(name = "assigned_by_id")
  private Long assignedById;

  @Column(name = "created_by_id")
  private Long createdById;

  @Column(name = "assigned_at")
  private LocalDateTime assignedAt;

  @Column(name = "completed_at")
  private LocalDateTime completedAt;

  @Column(name = "due_date")
  private LocalDateTime dueDate;

  @Column(name = "headquarter_id")
  private Long headquarterId;

  @Column(name = "project_id")
  private Long projectId;

  @Column(name = "opportunity_id")
  private Long opportunityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "checklist")
  private List<ChecklistItemJson> checklist = new ArrayList<>();

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Task.Status getStatus() {
    return status;
  }

  public void setStatus(Task.Status status) {
    this.status = status;
  }

  public Task.Priority getPriority() {
    return priority;
  }

  public void setPriority(Task.Priority priority) {
    this.priority = priority;
  }

  public Long getAssignedToId() {
    return assignedToId;
  }

  public void setAssignedToId(Long assignedToId) {
    this.assignedToId = assignedToId;
  }

  public Long getAssignedById() {
    return assignedById;
  }

  public void setAssignedById(Long assignedById) {
    this.assignedById = assignedById;
  }

  public Long getCreatedById() {
    return createdById;
  }

  public void setCreatedById(Long createdById) {
    this.createdById = createdById;
  }

  public LocalDateTime getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(LocalDateTime assignedAt) {
    this.assignedAt = assignedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public LocalDateTime getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDateTime dueDate) {
    this.dueDate = dueDate;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

  public List<ChecklistItemJson> getChecklist() {
    return checklist;
  }

  public void setChecklist(List<ChecklistItemJson> checklist) {
    this.checklist = checklist != null ? checklist : new ArrayList<>();
  }
}
