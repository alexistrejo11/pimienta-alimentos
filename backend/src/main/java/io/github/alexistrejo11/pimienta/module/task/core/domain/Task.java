package io.github.alexistrejo11.pimienta.module.task.core.domain;

import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Task aggregate: persistence-shaped, no business rules. Use cases orchestrate state changes;
 * format validation stays on Jakarta-annotated web DTOs. Build with {@link #builder()} and
 * {@link SafeBuilder#register()} or {@link SafeBuilder#reconstruct()}.
 */
public class Task extends BaseDomain<Long> {

  public enum Status {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    DELAYED,
    ON_HOLD,
    FAILED,
    /** Sentinel when the value is absent in storage. */
    UNDEFINED
  }

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
    UNDEFINED
  }

  public record ChecklistItem(
      String description, boolean isCompleted, LocalDateTime completedAt, int displayOrder) {}

  private String title;
  private String description;
  private Status status;
  private Priority priority;
  private Long assignedToId;
  private Long assignedById;
  private Long createdById;
  private LocalDateTime assignedAt;
  private LocalDateTime completedAt;
  private LocalDateTime dueDate;
  private Long headquarterId;
  private Long projectId;
  private Long opportunityId;
  private List<ChecklistItem> checklist = new ArrayList<>();

  private Task() {
    this.id = 0L;
    this.title = "";
    this.description = "";
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.deletedAt = null;
  }

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public void touch() {
    this.updatedAt = LocalDateTime.now();
  }

  public void delete() {
    this.deletedAt = LocalDateTime.now();
    touch();
  }

  public double getProgressPercentage() {
    if (checklist.isEmpty()) {
      return 0;
    }
    long completedCount = checklist.stream().filter(ChecklistItem::isCompleted).count();
    return (double) completedCount / checklist.size() * 100;
  }

  public String getTitle() {
    return title != null ? title : "";
  }

  public String getDescription() {
    return description != null ? description : "";
  }

  public Status getStatus() {
    return status != null ? status : Status.UNDEFINED;
  }

  public Priority getPriority() {
    return priority != null ? priority : Priority.UNDEFINED;
  }

  public Status getStatusOrNull() {
    return status;
  }

  public Priority getPriorityOrNull() {
    return priority;
  }

  public Long getAssignedToId() {
    return assignedToId;
  }

  public Long getAssignedById() {
    return assignedById;
  }

  public Long getCreatedById() {
    return createdById;
  }

  public LocalDateTime getAssignedAt() {
    return assignedAt;
  }

  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  public LocalDateTime getDueDate() {
    return dueDate;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public Long getProjectId() {
    return projectId;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public List<ChecklistItem> getChecklist() {
    return Collections.unmodifiableList(new ArrayList<>(checklist));
  }

  public void setTitle(String title) {
    this.title = title != null ? title.strip() : "";
  }

  public void setDescription(String description) {
    this.description = description != null ? description : "";
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setPriority(Priority priority) {
    this.priority = priority;
  }

  public void setAssignedToId(Long assignedToId) {
    this.assignedToId = assignedToId;
  }

  public void setAssignedById(Long assignedById) {
    this.assignedById = assignedById;
  }

  public void setCreatedById(Long createdById) {
    this.createdById = createdById;
  }

  public void setAssignedAt(LocalDateTime assignedAt) {
    this.assignedAt = assignedAt;
  }

  public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
  }

  public void setDueDate(LocalDateTime dueDate) {
    this.dueDate = dueDate;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

  /** Replaces checklist content (does not bump version; caller should {@link #touch()}). */
  public void setChecklist(List<ChecklistItem> items) {
    this.checklist = items != null ? new ArrayList<>(items) : new ArrayList<>();
  }

  public static final class SafeBuilder {
    private Long id;
    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private Long assignedToId;
    private Long assignedById;
    private Long createdById;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private LocalDateTime dueDate;
    private Long headquarterId;
    private Long projectId;
    private Long opportunityId;
    private final List<ChecklistItem> checklistDraft = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(Long id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withTitle(String title) {
      this.title = title != null ? title.strip() : "";
      return this;
    }

    public SafeBuilder withDescription(String description) {
      this.description = description != null ? description : "";
      return this;
    }

    public SafeBuilder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withPriority(Priority priority) {
      this.priority = priority;
      return this;
    }

    public SafeBuilder withAssignedToId(Long assignedToId) {
      this.assignedToId = assignedToId;
      return this;
    }

    public SafeBuilder withAssignedById(Long assignedById) {
      this.assignedById = assignedById;
      return this;
    }

    public SafeBuilder withCreatedById(Long createdById) {
      this.createdById = createdById;
      return this;
    }

    public SafeBuilder withAssignedAt(LocalDateTime assignedAt) {
      this.assignedAt = assignedAt;
      return this;
    }

    public SafeBuilder withCompletedAt(LocalDateTime completedAt) {
      this.completedAt = completedAt;
      return this;
    }

    public SafeBuilder withDueDate(LocalDateTime dueDate) {
      this.dueDate = dueDate;
      return this;
    }

    public SafeBuilder withHeadquarterId(Long headquarterId) {
      this.headquarterId = headquarterId;
      return this;
    }

    public SafeBuilder withProjectId(Long projectId) {
      this.projectId = projectId;
      return this;
    }

    public SafeBuilder withOpportunityId(Long opportunityId) {
      this.opportunityId = opportunityId;
      return this;
    }

    public SafeBuilder withChecklistDrafts(List<ChecklistDraft> lines) {
      checklistDraft.clear();
      if (lines == null) {
        return this;
      }
      for (ChecklistDraft line : lines) {
        String d = line.description() != null ? line.description().strip() : "";
        checklistDraft.add(new ChecklistItem(d, false, null, line.displayOrder()));
      }
      return this;
    }

    public SafeBuilder withChecklistItems(List<ChecklistItem> items) {
      checklistDraft.clear();
      if (items != null) {
        checklistDraft.addAll(items);
      }
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt != null ? updatedAt : LocalDateTime.now();
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

    public Task reconstruct() {
      Task t = new Task();
      t.id = id != null ? id : 0L;
      t.title = title != null ? title : "";
      t.description = description != null ? description : "";
      t.status = status;
      t.priority = priority;
      t.assignedToId = assignedToId;
      t.assignedById = assignedById;
      t.createdById = createdById;
      t.assignedAt = assignedAt;
      t.completedAt = completedAt;
      t.dueDate = dueDate;
      t.headquarterId = headquarterId;
      t.projectId = projectId;
      t.opportunityId = opportunityId;
      t.checklist = new ArrayList<>(checklistDraft);
      t.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      t.updatedAt = updatedAt != null ? updatedAt : t.createdAt;
      t.deletedAt = deletedAt;
      t.version = version;
      return t;
    }

    /** New aggregate for insert (identity 0 until saved). */
    public Task register() {
      Task t = new Task();
      t.id = 0L;
      t.title = title != null ? title : "";
      t.description = description != null ? description : "";
      t.status = status;
      t.priority = priority;
      t.assignedToId = assignedToId;
      t.assignedById = assignedById;
      t.createdById = createdById;
      t.assignedAt = assignedAt;
      t.completedAt = completedAt;
      t.dueDate = dueDate;
      t.headquarterId = headquarterId;
      t.projectId = projectId;
      t.opportunityId = opportunityId;
      t.checklist = new ArrayList<>(checklistDraft);
      LocalDateTime now = LocalDateTime.now();
      t.createdAt = createdAt != null ? createdAt : now;
      t.updatedAt = updatedAt != null ? updatedAt : now;
      t.deletedAt = deletedAt;
      t.version = version;
      return t;
    }

    public Task build() {
      return reconstruct();
    }
  }
}
