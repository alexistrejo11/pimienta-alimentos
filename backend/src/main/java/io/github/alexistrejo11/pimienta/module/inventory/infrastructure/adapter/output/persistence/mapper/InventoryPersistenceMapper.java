package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity.InventoryJpaEntity;

import org.springframework.stereotype.Component;

@Component
public class InventoryPersistenceMapper {

  private final ItemRepository itemRepository;
  private final StorageLocationRepository storageLocationRepository;

  public InventoryPersistenceMapper(
      ItemRepository itemRepository, StorageLocationRepository storageLocationRepository) {
    this.itemRepository = itemRepository;
    this.storageLocationRepository = storageLocationRepository;
  }

  public InventoryJpaEntity toJpa(Inventory domain) {
    InventoryJpaEntity e = new InventoryJpaEntity();
    if (domain.getId() != null && domain.getId() > 0) {
      e.setId(domain.getId());
    }
    e.setItemId(domain.getItem().getId());
    e.setLocationId(domain.getLocation().getId());
    e.setAvailableQuantity(domain.getAvailableQuantity());
    e.setReservedQuantity(domain.getReservedQuantity());
    e.setInTransitQuantity(domain.getInTransitQuantity());
    e.setStatus(domain.getStatus());
    e.setCreatedAt(domain.getCreatedAt());
    e.setUpdatedAt(domain.getUpdatedAt());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());
    return e;
  }

  public Inventory toDomain(InventoryJpaEntity e) {
    Item item = itemRepository
        .findById(e.getItemId())
        .orElseThrow(
            () -> new IllegalStateException(
                "Item " + e.getItemId() + " missing for inventory " + e.getId()));
    StorageLocation location = storageLocationRepository
        .findById(e.getLocationId())
        .orElseThrow(
            () -> new IllegalStateException(
                "Location " + e.getLocationId() + " missing for inventory " + e.getId()));
    Inventory inv = new Inventory();
    inv.setId(e.getId());
    inv.setItem(item);
    inv.setLocation(location);
    inv.setAvailableQuantity(e.getAvailableQuantity());
    inv.setReservedQuantity(e.getReservedQuantity());
    inv.setInTransitQuantity(e.getInTransitQuantity());
    inv.setStatus(e.getStatus());
    inv.setCreatedAt(e.getCreatedAt());
    inv.setUpdatedAt(e.getUpdatedAt());
    inv.setDeletedAt(e.getDeletedAt());
    inv.setVersion(e.getVersion());
    return inv;
  }
}
