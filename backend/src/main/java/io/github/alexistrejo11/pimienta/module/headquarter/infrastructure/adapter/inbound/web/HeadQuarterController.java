package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreateHeadquarterCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpdateHeadquarterCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.Headquarter;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterStatistics;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterBulkSyncUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterCreate;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterDelete;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterExport;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterGetById;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterGetByName;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterImport;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterList;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterStatistics;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarterUpdate;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.doc.DocHeadquarters;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadQuarterRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadQuarterResponse;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterStatisticsResponse;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/headquarters")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocHeadquarters
public class HeadQuarterController {

  private final HeadquarterUseCases headquarterUseCases;
  private final HeadquarterBulkSyncUseCases headquarterBulkSyncUseCases;

  public HeadQuarterController(
      HeadquarterUseCases headquarterUseCases,
      HeadquarterBulkSyncUseCases headquarterBulkSyncUseCases) {
    this.headquarterUseCases = headquarterUseCases;
    this.headquarterBulkSyncUseCases = headquarterBulkSyncUseCases;
  }

  @GetMapping("/statistics")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterStatistics
  public HeadquarterStatisticsResponse getHeadquarterStatistics() {
    HeadquarterStatistics statistics = headquarterUseCases.statistics();
    return HeadQuarterWebMapper.toResponse(statistics);
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterList
  public Page<HeadQuarterResponse> getAllHeadquarters(@ModelAttribute PageableRequest pageable) {
    Page<Headquarter> headquarters = headquarterUseCases.getBy(pageable.toPageable());
    return headquarters.map(HeadQuarterWebMapper::toResponse);
  }

  @GetMapping("/export")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterExport
  public ResponseEntity<byte[]> exportHeadquarters(@ModelAttribute PageableRequest pageableRequest)
      throws IOException {
    Pageable pageable = pageableRequest.toPageable();
    byte[] bytes = headquarterBulkSyncUseCases.exportHeadquarters(pageable);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sedes_reporte.xlsx")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(bytes);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterImport
  public SpreadsheetBulkImportResult importHeadquarters(
      @RequestParam("file") MultipartFile file,
      @RequestParam(name = "dryRun", defaultValue = "false") boolean dryRun)
      throws IOException {
    if (file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
    }
    return headquarterBulkSyncUseCases.importHeadquarters(
        file.getInputStream(), file.getOriginalFilename(), dryRun);
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterGetById
  public HeadQuarterResponse getHeadquarterById(@PathVariable Long id) {
    Headquarter headquarter = headquarterUseCases.getById(id);
    return HeadQuarterWebMapper.toResponse(headquarter);
  }

  @GetMapping("/name/{name}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocHeadquarterGetByName
  public HeadQuarterResponse getHeadquarterByName(@PathVariable String name) {
    Headquarter headquarter = headquarterUseCases.getByName(name);
    return HeadQuarterWebMapper.toResponse(headquarter);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocHeadquarterCreate
  public HeadQuarterResponse createHeadquarter(@Valid @RequestBody HeadQuarterRequest request) {
    CreateHeadquarterCommand command = HeadQuarterWebMapper.toCreateCommand(request);
    Headquarter headquarter = headquarterUseCases.create(command);
    return HeadQuarterWebMapper.toResponse(headquarter);
  }

  @PutMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterUpdate
  public HeadQuarterResponse updateHeadquarter(
      @PathVariable Long id,
      @Valid @RequestBody HeadQuarterRequest request) {
    UpdateHeadquarterCommand command = HeadQuarterWebMapper.toUpdateCommand(request);
    Headquarter headquarter = headquarterUseCases.update(id, command);
    return HeadQuarterWebMapper.toResponse(headquarter);
  }

  @DeleteMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocHeadquarterDelete
  public ResponseEntity<Void> softDeleteHeadquarter(@PathVariable Long id) {
    headquarterUseCases.delete(id);
    return ResponseEntity.noContent().build();
  }
}
