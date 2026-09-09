package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.LocationTreeNode;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.StorageLocationCreateRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.StorageLocationUpdateRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.LocationTreeNodeResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.StorageLocationResponse;
import java.util.List;

public final class StorageLocationWebMapper {

  private StorageLocationWebMapper() {}

  public static StorageLocation toDomain(StorageLocationCreateRequest request) {
    return StorageLocation.create(
        request.code(),
        request.name(),
        request.description() != null ? request.description() : "",
        request.type(),
        request.parentId(),
        request.maxCapacity());
  }

  public static StorageLocation toMergedDomain(StorageLocationUpdateRequest request) {
    StorageLocation merged = new StorageLocation();
    merged.setCode(request.code());
    merged.setName(request.name());
    merged.setDescription(request.description() != null ? request.description() : "");
    merged.setType(request.type());
    merged.setParentId(request.parentId());
    merged.setMaxCapacity(request.maxCapacity());
    merged.setOccupiedCapacity(request.occupiedCapacity());
    merged.setStatus(request.status());
    return merged;
  }

  public static StorageLocationResponse toResponse(StorageLocation location) {
    return new StorageLocationResponse(
        location.getId(),
        location.getCode(),
        location.getName(),
        location.getDescription(),
        location.getType(),
        location.getParentId(),
        location.getHeadquarterId(),
        location.getMaxCapacity(),
        location.getOccupiedCapacity(),
        location.getStatus(),
        location.getCreatedAt(),
        location.getUpdatedAt(),
        location.getDeletedAt(),
        location.getVersion());
  }

  public static LocationTreeNodeResponse toTreeResponse(LocationTreeNode node) {
    List<LocationTreeNodeResponse> children =
        node.children().stream().map(StorageLocationWebMapper::toTreeResponse).toList();
    return new LocationTreeNodeResponse(
        node.id(),
        node.code(),
        node.name(),
        node.type(),
        node.status(),
        node.parentId(),
        node.maxCapacity(),
        node.occupiedCapacity(),
        children);
  }
}
