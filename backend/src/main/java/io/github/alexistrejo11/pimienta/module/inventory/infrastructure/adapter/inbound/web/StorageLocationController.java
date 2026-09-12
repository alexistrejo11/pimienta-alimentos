package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.LocationTreeNode;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.StorageLocationManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationBlock;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationChildren;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationCreate;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationDelete;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationGetById;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationSearch;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationTree;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationUnblock;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocationUpdate;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc.DocStorageLocations;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.StorageLocationCreateRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.StorageLocationSearchRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.StorageLocationUpdateRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.LocationTreeNodeResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.StorageLocationResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper.StorageLocationWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(BASE + "/inventory/locations")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocStorageLocations
public class StorageLocationController {

  private final StorageLocationManagementUseCases storageLocationManagementUseCases;
  private final HeadquarterAccessService headquarterAccessService;

  public StorageLocationController(
      StorageLocationManagementUseCases storageLocationManagementUseCases,
      HeadquarterAccessService headquarterAccessService) {
    this.storageLocationManagementUseCases = storageLocationManagementUseCases;
    this.headquarterAccessService = headquarterAccessService;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocStorageLocationSearch
  public PagedResponse<StorageLocationResponse> searchLocations(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @ParameterObject @ModelAttribute StorageLocationSearchRequest filter) {
    Long hq = headquarterAccessService.enforceHeadquarterFilter(principal, filter.getHeadquarterId());
    Page<StorageLocation> page =
        storageLocationManagementUseCases.search(filter.toCriteria(hq), filter.toPageable());
    return PagedResponse.map(page, StorageLocationWebMapper::toResponse);
  }

  @GetMapping("/tree")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocStorageLocationTree
  public List<LocationTreeNodeResponse> getLocationTree() {
    List<LocationTreeNode> tree = storageLocationManagementUseCases.getTree();
    return tree.stream().map(StorageLocationWebMapper::toTreeResponse).toList();
  }

  @GetMapping("/parent/{parentId}/children")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocStorageLocationChildren
  public List<StorageLocationResponse> getChildren(@PathVariable Long parentId) {
    List<StorageLocation> children = storageLocationManagementUseCases.getChildren(parentId);
    return children.stream().map(StorageLocationWebMapper::toResponse).toList();
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocStorageLocationGetById
  public StorageLocationResponse getLocationById(@PathVariable Long id) {
    StorageLocation location = storageLocationManagementUseCases.getById(id);
    return StorageLocationWebMapper.toResponse(location);
  }

  @PostMapping
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @ResponseStatus(HttpStatus.CREATED)
  @DocStorageLocationCreate
  public StorageLocationResponse createLocation(@Valid @RequestBody StorageLocationCreateRequest request) {
    StorageLocation created = storageLocationManagementUseCases.create(StorageLocationWebMapper.toDomain(request));
    return StorageLocationWebMapper.toResponse(created);
  }

  @PutMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocStorageLocationUpdate
  public StorageLocationResponse updateLocation(
      @PathVariable Long id, @Valid @RequestBody StorageLocationUpdateRequest request) {
    StorageLocation merged = StorageLocationWebMapper.toMergedDomain(request);
    StorageLocation updated = storageLocationManagementUseCases.update(id, merged);
    return StorageLocationWebMapper.toResponse(updated);
  }

  @PutMapping("/{id}/block")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocStorageLocationBlock
  public StorageLocationResponse blockLocation(@PathVariable Long id) {
    StorageLocation location = storageLocationManagementUseCases.block(id);
    return StorageLocationWebMapper.toResponse(location);
  }

  @PutMapping("/{id}/unblock")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocStorageLocationUnblock
  public StorageLocationResponse unblockLocation(@PathVariable Long id) {
    StorageLocation location = storageLocationManagementUseCases.unblock(id);
    return StorageLocationWebMapper.toResponse(location);
  }

  @DeleteMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocStorageLocationDelete
  public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
    storageLocationManagementUseCases.delete(id);
    return ResponseEntity.noContent().build();
  }
}
