package io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.files.core.application.command.UploadFileCommand;
import io.github.alexistrejo11.pimienta.module.files.core.application.query.FileAssetSearchCriteria;
import io.github.alexistrejo11.pimienta.module.files.core.domain.FileAsset;
import io.github.alexistrejo11.pimienta.module.files.core.domain.enums.FileCategory;
import io.github.alexistrejo11.pimienta.module.files.core.port.input.FileAssetManagementUseCases;
import io.github.alexistrejo11.pimienta.module.files.core.port.input.FileAssetQueryUseCases;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileResources;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileResourcesDownload;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileResourcesSearch;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.doc.DocFileResourcesUpload;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.dto.FileAssetResponse;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.dto.FileDownloadUrlResponse;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.dto.FileResourceSearchRequest;
import io.github.alexistrejo11.pimienta.module.files.infrastructure.adapter.inbound.web.mapper.FileAssetWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/files/resources")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocFileResources
@PreAuthorize("hasRole('ADMIN')")
public class FileResourcesController {

  private final FileAssetManagementUseCases managementUseCases;
  private final FileAssetQueryUseCases queryUseCases;

  public FileResourcesController(
      FileAssetManagementUseCases managementUseCases,
      FileAssetQueryUseCases queryUseCases) {
    this.managementUseCases = managementUseCases;
    this.queryUseCases = queryUseCases;
  }

  @PostMapping(value = "/upload", consumes = "multipart/form-data")
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocFileResourcesUpload
  public FileAssetResponse upload(
      @RequestParam("file") MultipartFile file,
      @RequestParam("module") String module,
      @RequestParam(value = "entityType", required = false) String entityType,
      @RequestParam(value = "entityId", required = false) Long entityId,
      @RequestParam(value = "description", required = false) String description,
      @RequestParam(value = "uploadedByUserId", required = false) Long uploadedByUserId) {
    UploadFileCommand command = new UploadFileCommand(
        file, FileCategory.RESOURCE, module, entityType, entityId, description, uploadedByUserId);
    return FileAssetWebMapper.toResponse(managementUseCases.upload(command));
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocFileResourcesSearch
  public PagedResponse<FileAssetResponse> search(
      @Valid @ModelAttribute FileResourceSearchRequest filter) {
    FileAssetSearchCriteria criteria = FileAssetWebMapper.toResourceCriteria(filter);
    Page<FileAsset> page = queryUseCases.search(criteria, filter.toPageable());
    return PagedResponse.map(page, FileAssetWebMapper::toResponse);
  }

  @GetMapping("/{id}/download-url")
  @DocFileResourcesDownload
  public FileDownloadUrlResponse downloadUrl(@PathVariable UUID id) {
    return new FileDownloadUrlResponse(id, managementUseCases.generateDownloadUrl(id));
  }
}
