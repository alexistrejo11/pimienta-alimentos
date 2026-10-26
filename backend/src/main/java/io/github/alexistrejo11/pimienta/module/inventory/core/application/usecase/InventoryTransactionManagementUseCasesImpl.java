package io.github.alexistrejo11.pimienta.module.inventory.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.AdjustmentLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.AdjustmentTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.InLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.InTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.OutLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.OutTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryTransactionSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryTransactionInvalidStateException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryTransactionNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.StorageLocationNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryTransactionManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryMovementRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryTransactionRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryTransactionManagementUseCasesImpl implements InventoryTransactionManagementUseCases {

  private static final Logger log = LoggerFactory.getLogger(InventoryTransactionManagementUseCasesImpl.class);

  private final InventoryTransactionRepository transactionRepository;
  private final InventoryMovementRepository movementRepository;
  private final InventoryRepository inventoryRepository;
  private final ItemRepository itemRepository;
  private final StorageLocationRepository storageLocationRepository;

  public InventoryTransactionManagementUseCasesImpl(
      InventoryTransactionRepository transactionRepository,
      InventoryMovementRepository movementRepository,
      InventoryRepository inventoryRepository,
      ItemRepository itemRepository,
      StorageLocationRepository storageLocationRepository) {
    this.transactionRepository = transactionRepository;
    this.movementRepository = movementRepository;
    this.inventoryRepository = inventoryRepository;
    this.itemRepository = itemRepository;
    this.storageLocationRepository = storageLocationRepository;
  }

  @Override
  public Page<InventoryTransaction> search(InventoryTransactionSearchCriteria criteria, Pageable pageable) {
    InventoryTransactionSearchCriteria effective =
        criteria != null ? criteria : InventoryTransactionSearchCriteria.empty();
    return transactionRepository.search(effective, pageable);
  }

  @Override
  public InventoryTransaction getById(Long id) {
    return transactionRepository
        .findById(id)
        .orElseThrow(() -> new InventoryTransactionNotFoundException(id));
  }

  @Override
  @Transactional
  public InventoryTransaction receive(InTransactionCommand command) {
    InventoryTransaction tx =
        InventoryTransaction.open(
            newTransactionNumber(),
            TransactionType.IN,
            command.externalReference(),
            command.notes(),
            command.initiatedById());
    tx = transactionRepository.save(tx);

    for (InLine line : command.lines()) {
      if (line.quantity() <= 0) {
        throw new BusinessValidationException(
            "La cantidad de entrada debe ser mayor a cero.",
            Map.of("itemId", line.itemId()),
            "non-positive IN quantity");
      }
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      inv.addStock(line.quantity());
      inventoryRepository.save(inv);
      InventoryMovement movement =
          InventoryMovement.recordIn(
              inv.getItem(),
              inv.getLocation(),
              line.quantity(),
              line.unitCost(),
              null,
              command.externalReference(),
              "Stock in",
              command.initiatedById(),
              inv.getAvailableQuantity());
      movement.setTransactionId(tx.getId());
      movementRepository.save(movement);
    }
    return finishCompleted(tx);
  }

  @Override
  @Transactional
  public InventoryTransaction exit(OutTransactionCommand command) {
    if (command.exitReason() != InventoryExitReason.WASTE
        && command.exitReason() != InventoryExitReason.WITHDRAWAL) {
      throw new BusinessValidationException(
          "El motivo de salida debe ser WASTE o WITHDRAWAL.",
          Map.of("exitReason", String.valueOf(command.exitReason())),
          "staff OUT reason");
    }
    InventoryTransaction tx =
        InventoryTransaction.open(
            newTransactionNumber(),
            TransactionType.OUT,
            command.externalReference(),
            command.notes(),
            command.initiatedById());
    tx.setExitReason(command.exitReason());
    tx = transactionRepository.save(tx);

    for (OutLine line : command.lines()) {
      if (line.quantity() <= 0) {
        throw new BusinessValidationException(
            "La cantidad de salida debe ser mayor a cero.",
            Map.of("itemId", line.itemId()),
            "non-positive OUT quantity");
      }
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      inv.applyUnrestrictedDelta(-line.quantity());
      inventoryRepository.save(inv);
      BigDecimal unitCost = line.unitCost() != null ? line.unitCost() : BigDecimal.ZERO;
      InventoryMovement movement =
          InventoryMovement.recordOut(
              inv.getItem(),
              inv.getLocation(),
              line.quantity(),
              unitCost,
              command.exitReason(),
              command.externalReference(),
              command.exitReason().name(),
              command.initiatedById(),
              inv.getAvailableQuantity());
      movement.setTransactionId(tx.getId());
      movementRepository.save(movement);
    }
    return finishCompleted(tx);
  }

  @Override
  @Transactional
  public InventoryTransaction adjustment(AdjustmentTransactionCommand command) {
    InventoryTransaction tx =
        InventoryTransaction.open(
            newTransactionNumber(),
            TransactionType.ADJUSTMENT,
            command.externalReference(),
            command.notes(),
            command.initiatedById());
    tx = transactionRepository.save(tx);

    for (AdjustmentLine line : command.lines()) {
      if (line.countedQuantity() < 0) {
        throw new BusinessValidationException(
            "La cantidad contada no puede ser negativa.",
            Map.of("itemId", line.itemId()),
            "negative counted quantity");
      }
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      int before = inv.getAvailableQuantity();
      inv.adjust(line.countedQuantity());
      inventoryRepository.save(inv);
      int delta = line.countedQuantity() - before;
      if (delta != 0) {
        InventoryMovement movement =
            InventoryMovement.recordAdjustment(
                inv.getItem(),
                inv.getLocation(),
                Math.abs(delta),
                delta > 0,
                command.externalReference(),
                line.reason(),
                command.initiatedById(),
                inv.getAvailableQuantity());
        movement.setTransactionId(tx.getId());
        movementRepository.save(movement);
      }
    }
    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory adjustment complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  private String newTransactionNumber() {
    return "TXN-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(10000, 99999);
  }

  private Inventory getOrCreateInventory(long itemId, long locationId) {
    return inventoryRepository
        .findByItemIdAndLocationId(itemId, locationId)
        .orElseGet(
            () -> {
              Item item =
                  itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
              StorageLocation loc =
                  storageLocationRepository
                      .findById(locationId)
                      .orElseThrow(() -> new StorageLocationNotFoundException(locationId));
              if (loc.getStatus() == LocationStatus.BLOCKED) {
                throw new InventoryTransactionInvalidStateException(
                    -1L, "Location " + locationId + " is blocked");
              }
              Inventory inv = Inventory.create(item, loc, 0);
              return inventoryRepository.save(inv);
            });
  }

  private void assertLocationWritable(long locationId) {
    StorageLocation loc =
        storageLocationRepository
            .findById(locationId)
            .orElseThrow(() -> new StorageLocationNotFoundException(locationId));
    if (loc.getStatus() == LocationStatus.BLOCKED) {
      throw new InventoryTransactionInvalidStateException(-1L, "Location " + locationId + " is blocked");
    }
  }

  private InventoryTransaction finishCompleted(InventoryTransaction tx) {
    InventoryTransaction loaded = transactionRepository.findById(tx.getId()).orElseThrow();
    loaded.complete();
    return transactionRepository.save(loaded);
  }
}
