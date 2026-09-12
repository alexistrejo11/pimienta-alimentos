package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.module.files.core.application.command.UploadFileCommand;
import io.github.alexistrejo11.pimienta.module.files.core.application.query.FileAssetSearchCriteria;
import io.github.alexistrejo11.pimienta.module.files.core.domain.FileAsset;
import io.github.alexistrejo11.pimienta.module.files.core.domain.enums.FileCategory;
import io.github.alexistrejo11.pimienta.module.files.core.port.input.FileAssetManagementUseCases;
import io.github.alexistrejo11.pimienta.module.files.core.port.input.FileAssetQueryUseCases;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileManagement;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileManagementDelete;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileManagementDownload;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileManagementGetById;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileManagementSearch;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileManagementUpload;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.dto.FileAssetResponse;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.dto.FileAssetSearchRequest;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.dto.FileDownloadUrlResponse;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.mapper.FileAssetWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(BASE + "/files/management")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocFileManagement
@PreAuthorize("hasRole('ADMIN')")
public class FileManagementController {

  private final FileAssetManagementUseCases managementUseCases;
  private final FileAssetQueryUseCases queryUseCases;

  public FileManagementController(
      FileAssetManagementUseCases managementUseCases,
      FileAssetQueryUseCases queryUseCases) {
    this.managementUseCases = managementUseCases;
    this.queryUseCases = queryUseCases;
  }

  @PostMapping(value = "/upload", consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocFileManagementUpload
  public FileAssetResponse upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("category") FileCategory category,
      @RequestParam(value = "module", required = false) String module,
      @RequestParam(value = "entityType", required = false) String entityType,
      @RequestParam(value = "entityId", required = false) Long entityId,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "uploadedByUserId", required = false) Long uploadedByUserId) {
    UploadFileCommand command = new UploadFileCommand(
        file, category, module, entityType, entityId, description, uploadedByUserId);
    return FileAssetWebMapper.toResponse(managementUseCases.upload(command));
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocFileManagementSearch
  public PagedResponse<FileAssetResponse> search(@Valid @ModelAttribute FileAssetSearchRequest filter) {
    FileAssetSearchCriteria criteria = FileAssetWebMapper.toAdminCriteria(filter);
    Page<FileAsset> page = queryUseCases.search(criteria, filter.toPageable());
    return PagedResponse.map(page, FileAssetWebMapper::toResponse);
  }

  @GetMapping("/{id}")
  @DocFileManagementGetById
  public FileAssetResponse getById(@PathVariable UUID id) {
    return FileAssetWebMapper.toResponse(queryUseCases.getById(id));
  }

  @GetMapping("/{id}/download-url")
  @DocFileManagementDownload
  public FileDownloadUrlResponse downloadUrl(@PathVariable UUID id) {
    return new FileDownloadUrlResponse(id, managementUseCases.generateDownloadUrl(id));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocFileManagementDelete
  public void delete(@PathVariable UUID id) {
    managementUseCases.delete(id);
  }
}
