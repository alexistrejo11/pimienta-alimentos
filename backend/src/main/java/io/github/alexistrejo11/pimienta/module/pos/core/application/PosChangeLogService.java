package io.github.alexistrejo11.pimienta.module.pos.core.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosChangeLogRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosChangeLogRepository.PosChangeLogEntry;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Appends durable POS projections in the caller's transaction. */
@Service
public class PosChangeLogService {

  private final PosChangeLogRepository repository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public PosChangeLogService(PosChangeLogRepository repository) {
    this.repository = repository;
  }

  public void appendCatalogItem(HeadquarterItem item) {
    append(
        item.getHeadquarterId(),
        "CATALOG_ITEM",
        String.valueOf(item.getItemId()),
        item.getDeletedAt() == null ? "UPSERT" : "DEACTIVATE",
        Map.of("itemId", item.getItemId()));
  }

  public void appendInventoryStock(Inventory inventory) {
    if (inventory.getLocation() == null || inventory.getLocation().getHeadquarterId() == null) {
      return;
    }
    if (inventory.getLocation().getType() != null
        && "POS".equals(inventory.getLocation().getType().name())) {
      append(
          inventory.getLocation().getHeadquarterId(),
          "INVENTORY_STOCK",
          String.valueOf(inventory.getItem().getId()),
          "UPSERT",
          Map.of("itemId", inventory.getItem().getId()));
    }
  }

  public void appendOperator(long headquarterId, long operatorId, String operation) {
    append(
        headquarterId,
        "OPERATOR",
        String.valueOf(operatorId),
        operation,
        Map.of("operatorId", operatorId));
  }

  public void appendPolicy(long headquarterId, Object projection) {
    append(headquarterId, "POLICY", String.valueOf(headquarterId), "UPSERT", projection);
  }

  public void appendCatalogItemsForGlobalItem(long itemId, List<HeadquarterItem> catalogItems) {
    catalogItems.forEach(this::appendCatalogItem);
  }

  private void append(
      long headquarterId, String entityType, String entityId, String operation, Object payload) {
    try {
      repository.save(
          new PosChangeLogEntry(
              null,
              headquarterId,
              entityType,
              entityId,
              operation,
              objectMapper.writeValueAsString(payload),
              null));
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Could not serialize POS change projection", ex);
    }
  }
}
