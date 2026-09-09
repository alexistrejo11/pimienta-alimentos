package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationType;
import java.time.LocalDateTime;

public record StorageLocationResponse(
    Long id,
    String code,
    String name,
    String description,
    LocationType type,
    Long parentId,
    Long headquarterId,
    int maxCapacity,
    int occupiedCapacity,
    LocationStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt,
    Long version) {}
