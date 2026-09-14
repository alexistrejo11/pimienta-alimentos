package io.github.alexistrejo11.pimienta.module.inventory.core.application.query;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import java.util.List;

public record StorageLocationSearchCriteria(
    StorageLocation.LocationType type,
    StorageLocation.LocationStatus status,
    List<Long> headquarterIds) {

  public static StorageLocationSearchCriteria empty() {
    return new StorageLocationSearchCriteria(null, null, null);
  }
}
