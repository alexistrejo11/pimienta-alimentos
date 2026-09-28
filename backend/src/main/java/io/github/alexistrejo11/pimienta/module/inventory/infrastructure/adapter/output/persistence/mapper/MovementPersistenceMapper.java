package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryMovementJpaEntity;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class MovementPersistenceMapper {

  private final ItemRepository itemRepository;
  private final StorageLocationRepository storageLocationRepository;

  public MovementPersistenceMapper(
      ItemRepository itemRepository, StorageLocationRepository storageLocationRepository) {
    this.itemRepository = itemRepository;
    this.storageLocationRepository = storageLocationRepository;
  }

  public InventoryMovementJpaEntity toJpa(InventoryMovement domain) {
    InventoryMovementJpaEntity e = new InventoryMovementJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setItemId(domain.getItem().getId());
    e.setSourceLocationId(domain.getSourceLocation() != null ? domain.getSourceLocation().getId() : null);
    e.setDestinationLocationId(
        domain.getDestinationLocation() != null ? domain.getDestinationLocation().getId() : null);
    e.setTransactionId(domain.getTransactionId());
    e.setQuantity(domain.getQuantity());
    e.setUnitCost(domain.getUnitCost() != null ? domain.getUnitCost() : BigDecimal.ZERO);
    e.setType(domain.getType());
    e.setDirection(domain.getDirection());
    e.setDescription(domain.getDescription());
    e.setReferenceNumber(domain.getReferenceNumber());
    e.setPerformedById(domain.getPerformedById());
    e.setStockAfterMovement(domain.getStockAfterMovement());
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  public InventoryMovement toDomain(InventoryMovementJpaEntity e) {
    Item item = itemRepository
        .findById(e.getItemId())
        .orElseThrow(() -> new IllegalStateException("Item " + e.getItemId() + " for movement"));
    StorageLocation source = e.getSourceLocationId() != null
        ? storageLocationRepository.findById(e.getSourceLocationId()).orElse(null)
        : null;
    StorageLocation dest = e.getDestinationLocationId() != null
        ? storageLocationRepository.findById(e.getDestinationLocationId()).orElse(null)
        : null;
    InventoryMovement movement = new InventoryMovement();
    movement.setId(e.getId());
    movement.setItem(item);
    movement.setSourceLocation(source);
    movement.setDestinationLocation(dest);
    movement.setTransactionId(e.getTransactionId());
    movement.setQuantity(e.getQuantity());
    movement.setUnitCost(e.getUnitCost());
    movement.setType(e.getType());
    movement.setDirection(e.getDirection());
    movement.setDescription(e.getDescription());
    movement.setReferenceNumber(e.getReferenceNumber());
    movement.setPerformedById(e.getPerformedById());
    movement.setStockAfterMovement(e.getStockAfterMovement());
    movement.setCreatedAt(e.getCreatedAt());
    movement.setUpdatedAt(e.getUpdatedAt());
    movement.setVersion(e.getVersion());
    return movement;
  }
}
