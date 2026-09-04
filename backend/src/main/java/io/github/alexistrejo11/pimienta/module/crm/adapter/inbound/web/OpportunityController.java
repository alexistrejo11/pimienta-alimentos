package io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunities;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityAbandon;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityCreate;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityCreateTask;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityDelete;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityExport;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityGetById;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityGetSummary;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityImport;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityListTasks;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityLose;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityPipelineDiscovery;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityPipelineNegotiation;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityPipelineProposal;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityReopen;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunitySearch;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityUpdate;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.doc.DocOpportunityWin;
import io.github.alexistrejo11.pimienta.module.crm.core.application.OpportunityBulkSyncUseCases;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.CreateOpportunityParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.summary.OpportunitySummary;
import io.github.alexistrejo11.pimienta.module.crm.core.port.input.OpportunityUseCases;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.CreateOpportunityRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.LoseOpportunityRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.OpportunityResponse;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.OpportunitySearchRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.OpportunitySummaryResponse;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.UpdateOpportunityRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.dto.WinOpportunityRequest;
import io.github.alexistrejo11.pimienta.module.crm.adapter.inbound.web.mapper.OpportunityWebMapper;
import io.github.alexistrejo11.pimienta.module.task.core.application.TaskManagementUseCases;
import io.github.alexistrejo11.pimienta.module.task.core.application.query.TaskSearchCriteria;
import io.github.alexistrejo11.pimienta.module.task.core.application.command.CreateTaskCommand;
import io.github.alexistrejo11.pimienta.module.task.core.domain.Task;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.TaskManagerWebMapper;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskListItemResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskRequest;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskResponse;
import io.github.alexistrejo11.pimienta.module.task.infrastructure.adapter.inbound.web.dto.TaskSearchRequest;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/opportunities")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocOpportunities
public class OpportunityController {

  private final OpportunityUseCases opportunityUseCases;
  private final OpportunityBulkSyncUseCases opportunityBulkSyncUseCases;
  private final TaskManagementUseCases taskManagementUseCases;

  public OpportunityController(
      OpportunityUseCases opportunityUseCases,
      OpportunityBulkSyncUseCases opportunityBulkSyncUseCases,
      TaskManagementUseCases taskManagementUseCases) {
    this.opportunityUseCases = opportunityUseCases;
    this.opportunityBulkSyncUseCases = opportunityBulkSyncUseCases;
    this.taskManagementUseCases = taskManagementUseCases;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOpportunitySearch
  public PagedResponse<OpportunityResponse> searchOpportunities(
      @ModelAttribute OpportunitySearchRequest filter) {
    OpportunitySearchCriteria criteria = OpportunityWebMapper.toCriteria(filter);
    Pageable pageable = filter.toPageable();
    Page<Opportunity> page = opportunityUseCases.search(criteria, pageable);

    return PagedResponse.map(page, OpportunityWebMapper::toResponse);
  }

  @GetMapping("/export")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOpportunityExport
  public ResponseEntity<byte[]> exportOpportunities(@ModelAttribute OpportunitySearchRequest filter)
      throws IOException {
    OpportunitySearchCriteria criteria = OpportunityWebMapper.toCriteria(filter);
    byte[] bytes =
        opportunityBulkSyncUseCases.exportOpportunities(criteria, filter.toPageable());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=oportunidades_reporte.xlsx")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityImport
  public SpreadsheetBulkImportResult importOpportunities(
      @RequestParam("file") MultipartFile file,
      @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun)
      throws IOException {
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
    }
    return opportunityBulkSyncUseCases.importOpportunities(
        file.getInputStream(), file.getOriginalFilename(), dryRun);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOpportunityGetById
  public OpportunityResponse getOpportunity(@PathVariable Long id) {
    Opportunity opportunity = opportunityUseCases.getById(id);
    return OpportunityWebMapper.toResponse(opportunity);
  }

  @GetMapping("/{id}/summary")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOpportunityGetSummary
  public OpportunitySummaryResponse getOpportunitySummary(@PathVariable Long id) {
    OpportunitySummary opportunitySummary = opportunityUseCases.getSummary(id);
    return OpportunityWebMapper.toSummaryResponse(opportunitySummary);
  }

  @GetMapping("/{id}/tasks")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOpportunityListTasks
  public PagedResponse<TaskListItemResponse> listOpportunityTasks(
      @PathVariable Long id, @ModelAttribute TaskSearchRequest filter) {
    // Validates exists
    opportunityUseCases.getById(id);

    filter.setOpportunityId(id);
    TaskSearchCriteria criteria = TaskManagerWebMapper.toCriteria(filter);
    Page<Task> page = taskManagementUseCases.search(criteria, filter.toPageable());

    return PagedResponse.map(page, TaskManagerWebMapper::toListItem);
  }

  @PostMapping("/{id}/tasks")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocOpportunityCreateTask
  public TaskResponse createTaskForOpportunity(
      @PathVariable Long id, @Valid @RequestBody TaskRequest request) {
    // Validates exists
    opportunityUseCases.getById(id);

    CreateTaskCommand command = TaskManagerWebMapper.toCreateCommandForOpportunity(request, id);
    Task created = taskManagementUseCases.create(command);

    return TaskManagerWebMapper.toResponse(created);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocOpportunityCreate
  public OpportunityResponse createOpportunity(@Valid @RequestBody CreateOpportunityRequest request) {
    CreateOpportunityParams params = OpportunityWebMapper.toCreateParams(request);
    Opportunity created = opportunityUseCases.create(params);

    return OpportunityWebMapper.toResponse(created);
  }

  @PatchMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityUpdate
  public OpportunityResponse updateOpportunity(
      @PathVariable Long id, @Valid @RequestBody UpdateOpportunityRequest request) {
    Opportunity updated =
        opportunityUseCases.update(id, OpportunityWebMapper.toUpdateParams(request));
    return OpportunityWebMapper.toResponse(updated);
  }

  @DeleteMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityDelete
  public ResponseEntity<Void> deleteOpportunity(@PathVariable Long id) {
    opportunityUseCases.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/pipeline/discovery")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityPipelineDiscovery
  public OpportunityResponse moveToDiscovery(@PathVariable Long id) {
    Opportunity o = opportunityUseCases.moveToDiscovery(id);
    return OpportunityWebMapper.toResponse(o);
  }

  @PostMapping("/{id}/pipeline/proposal")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityPipelineProposal
  public OpportunityResponse sendProposal(@PathVariable Long id) {
    Opportunity o = opportunityUseCases.sendProposal(id);
    return OpportunityWebMapper.toResponse(o);
  }

  @PostMapping("/{id}/pipeline/negotiation")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityPipelineNegotiation
  public OpportunityResponse startNegotiation(@PathVariable Long id) {
    Opportunity o = opportunityUseCases.startNegotiation(id);
    return OpportunityWebMapper.toResponse(o);
  }

  @PostMapping("/{id}/win")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityWin
  public OpportunityResponse winOpportunity(
      @PathVariable Long id, @Valid @RequestBody WinOpportunityRequest request) {
    Opportunity o = opportunityUseCases.win(id, OpportunityWebMapper.toWinParams(request));
    return OpportunityWebMapper.toResponse(o);
  }

  @PostMapping("/{id}/lose")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityLose
  public OpportunityResponse loseOpportunity(
      @PathVariable Long id, @Valid @RequestBody LoseOpportunityRequest request) {
    Opportunity o = opportunityUseCases.lose(id, request.reason());
    return OpportunityWebMapper.toResponse(o);
  }

  @PostMapping("/{id}/abandon")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityAbandon
  public OpportunityResponse abandonOpportunity(@PathVariable Long id) {
    Opportunity o = opportunityUseCases.abandon(id);
    return OpportunityWebMapper.toResponse(o);
  }

  @PostMapping("/{id}/reopen")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOpportunityReopen
  public OpportunityResponse reopenOpportunity(@PathVariable Long id) {
    Opportunity o = opportunityUseCases.reopen(id);
    return OpportunityWebMapper.toResponse(o);
  }
}
