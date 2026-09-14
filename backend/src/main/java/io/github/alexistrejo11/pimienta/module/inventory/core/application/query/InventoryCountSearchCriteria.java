package io.github.alexistrejo11.pimienta.module.inventory.core.application.query;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession.Status;
import java.util.List;

public record InventoryCountSearchCriteria(
    Long locationId, Status status, List<Long> headquarterIds) {}
