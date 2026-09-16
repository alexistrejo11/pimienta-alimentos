package io.github.alexistrejo11.pimienta.module.inventory.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.ApplyPosSaleStockCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement.InventoryMovementType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.PosLocationNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosLocationUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosSaleInventoryUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryMovementRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosSaleInventoryUseCasesImpl implements PosSaleInventoryUseCases {

  private static final Logger log = LoggerFactory.getLogger(PosSaleInventoryUseCasesImpl.class);

  private final PosLocationUseCases posLocationUseCases;
  private final ItemRepository itemRepository;
  private final InventoryRepository inventoryRepository;
  private final InventoryMovementRepository movementRepository;
  private final StorageLocationRepository storageLocationRepository;

  public PosSaleInventoryUseCasesImpl(
      PosLocationUseCases posLocationUseCases,
      ItemRepository itemRepository,
      InventoryRepository inventoryRepository,
      InventoryMovementRepository movementRepository,
      StorageLocationRepository storageLocationRepository) {
    this.posLocationUseCases = posLocationUseCases;
    this.itemRepository = itemRepository;
    this.inventoryRepository = inventoryRepository;
    this.movementRepository = movementRepository;
    this.storageLocationRepository = storageLocationRepository;
  }

  @Override
  @Transactional
  public Inventory applySaleStock(ApplyPosSaleStockCommand command) {
    if (command.quantityOut() == 0) {
      throw new IllegalArgumentException("quantityOut must be non-zero");
    }

    StorageLocation resolvedPosLocation =
        posLocationUseCases
            .findPosLocation(command.headquarterId())
            .orElseGet(() -> posLocationUseCases.ensurePosLocation(command.headquarterId()));

    if (!resolvedPosLocation.isPos()) {
      throw new PosLocationNotFoundException(command.headquarterId());
    }

    // Lock the stable parent row so concurrent first-time stock creation is also serialized.
    StorageLocation posLocation =
        storageLocationRepository
            .findByIdForUpdate(resolvedPosLocation.getId())
            .orElseThrow(() -> new PosLocationNotFoundException(command.headquarterId()));

    Item item =
        itemRepository
            .findById(command.itemId())
            .orElseThrow(() -> new ItemNotFoundException(command.itemId()));

    Inventory inv =
        inventoryRepository
            .findByItemIdAndLocationIdForUpdate(item.getId(), posLocation.getId())
            .orElseGet(
                () -> {
                  Inventory created = Inventory.create(item, posLocation, 0);
                  return inventoryRepository.save(created);
                });

    // Never call Inventory.removeStock() / web sale() — allow negatives on POS only.
    inv.setLocation(posLocation);
    inv.setItem(item);
    inv.applyPosSaleDelta(command.quantityOut());
    Inventory saved = inventoryRepository.save(inv);
    storageLocationRepository.save(posLocation);

    BigDecimal unitCost =
        command.unitCost() != null ? command.unitCost() : item.getCostPrice();
    String reference =
        command.eventId() != null && !command.eventId().isBlank()
            ? command.eventId().trim()
            : null;

    if (command.quantityOut() > 0) {
      movementRepository.save(
          InventoryMovement.recordExit(
              item,
              posLocation,
              command.quantityOut(),
              unitCost,
              InventoryMovementType.SALE,
              reference,
              "POS sale",
              command.performedById(),
              saved.getAvailableQuantity()));
    } else {
      int qtyIn = Math.abs(command.quantityOut());
      movementRepository.save(
          InventoryMovement.recordEntry(
              item,
              posLocation,
              qtyIn,
              unitCost,
              InventoryMovementType.RETURN_FROM_CLIENT,
              reference,
              "POS stock reverse",
              command.performedById(),
              saved.getAvailableQuantity()));
    }

    log.info(
        "POS sale stock applied headquarterId={} itemId={} quantityOut={} stockAfter={}",
        command.headquarterId(),
        command.itemId(),
        command.quantityOut(),
        saved.getAvailableQuantity());
    return saved;
  }
}
