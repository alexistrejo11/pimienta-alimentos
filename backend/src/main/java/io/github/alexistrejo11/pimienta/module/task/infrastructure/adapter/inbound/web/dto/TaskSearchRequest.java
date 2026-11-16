package io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Query parameters for global task search (Spring binds {@code ?headquarterId=} and related
 * keys).
 */
@Schema(description = "Filters and pagination for GET /api/v1/tasks.")
public class TaskSearchRequest extends PageableRequest {

  @Schema(description = "Filter by headquarter id.", example = "1")
  private Long headquarterId;

  @Schema(description = "Filter by assignee (employee) id.", example = "7")
  private Long employeeId;

  @Schema(description = "Filter by status.", example = "PENDING")
  private Task.Status status;

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public Long getEmployeeId() {
    return employeeId;
  }

  public void setEmployeeId(Long employeeId) {
    this.employeeId = employeeId;
  }

  public Task.Status getStatus() {
    return status;
  }

  public void setStatus(Task.Status status) {
    this.status = status;
  }
}
