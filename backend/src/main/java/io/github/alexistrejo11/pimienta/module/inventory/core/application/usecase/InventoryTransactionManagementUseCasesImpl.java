package io.github.alexistrejo11.pimienta.module.inventory.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.AdjustmentLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.AdjustmentTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.PhysicalAdjustmentCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.PurchaseLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.PurchaseTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ReturnClientCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ReturnClientLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ReturnSupplierCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ReturnSupplierLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.SaleLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.SaleTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ScrapCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.ScrapLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.TransferLine;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands.TransferTransactionCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryTransactionSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation.LocationStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryTransactionNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryTransactionInvalidStateException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.StorageLocationNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryTransactionManagementUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryMovementRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryTransactionRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;

import java.math.BigDecimal;
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
    InventoryTransactionSearchCriteria effective = criteria != null ? criteria
        : InventoryTransactionSearchCriteria.empty();

    log.debug(
        "search inventory transactions query start page={} size={} type={} status={} initiatedById={}",
        pageable != null ? pageable.getPageNumber() : null,
        pageable != null ? pageable.getPageSize() : null,
        effective.type(),
        effective.status(),
        effective.initiatedById());

    Page<InventoryTransaction> page = transactionRepository.search(effective, pageable);

    log.debug(
        "search inventory transactions query complete totalElements={} numberOfElements={}",
        page.getTotalElements(),
        page.getNumberOfElements());
    return page;
  }

  @Override
  public InventoryTransaction getById(Long id) {
    log.debug("get inventory transaction by id query start transactionId={}", id);

    InventoryTransaction tx = transactionRepository
        .findById(id)
        .orElseThrow(() -> new InventoryTransactionNotFoundException(id));

    log.debug("get inventory transaction by id query complete transactionId={}", tx.getId());
    return tx;
  }

  private String newTransactionNumber() {
    return "TXN-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(10000, 99999);
  }

  private Inventory getOrCreateInventory(long itemId, long locationId) {
    return inventoryRepository
        .findByItemIdAndLocationId(itemId, locationId)
        .orElseGet(
            () -> {
              Item item = itemRepository.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
              StorageLocation loc = storageLocationRepository
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
    StorageLocation loc = storageLocationRepository
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

  @Override
  @Transactional
  public InventoryTransaction purchase(PurchaseTransactionCommand command) {
    log.info(
        "inventory transaction purchase start lineCount={} initiatedById={} externalRefLen={} notesLen={}",
        command.lines().size(),
        command.initiatedById(),
        command.externalReference() == null ? 0 : command.externalReference().length(),
        command.notes() == null ? 0 : command.notes().length());

    InventoryTransaction tx = InventoryTransaction.open(
        newTransactionNumber(),
        TransactionType.PURCHASE_RECEIPT,
        command.externalReference(),
        command.notes(),
        command.initiatedById());
    tx = transactionRepository.save(tx);

    for (PurchaseLine line : command.lines()) {
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      inv.addStock(line.quantity());
      inventoryRepository.save(inv);
      InventoryMovement m = InventoryMovement.recordEntry(
          inv.getItem(),
          inv.getLocation(),
          line.quantity(),
          line.unitCost(),
          InventoryMovement.InventoryMovementType.PURCHASE,
          command.externalReference(),
          "Purchase receipt",
          command.initiatedById(),
          inv.getAvailableQuantity());
      m.setTransactionId(tx.getId());
      movementRepository.save(m);
    }

    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory transaction purchase complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  @Override
  @Transactional
  public InventoryTransaction sale(SaleTransactionCommand command) {
    log.info(
        "inventory transaction sale start lineCount={} initiatedById={} externalRefLen={} notesLen={}",
        command.lines().size(),
        command.initiatedById(),
        command.externalReference() == null ? 0 : command.externalReference().length(),
        command.notes() == null ? 0 : command.notes().length());

    InventoryTransaction tx = InventoryTransaction.open(
        newTransactionNumber(),
        TransactionType.SALE_DISPATCH,
        command.externalReference(),
        command.notes(),
        command.initiatedById());
    tx = transactionRepository.save(tx);

    for (SaleLine line : command.lines()) {
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      inv.removeStock(line.quantity());
      inventoryRepository.save(inv);
      InventoryMovement m = InventoryMovement.recordExit(
          inv.getItem(),
          inv.getLocation(),
          line.quantity(),
          line.unitCost(),
          InventoryMovement.InventoryMovementType.SALE,
          command.externalReference(),
          "Sale dispatch",
          command.initiatedById(),
          inv.getAvailableQuantity());
      m.setTransactionId(tx.getId());
      movementRepository.save(m);
    }

    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory transaction sale complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  @Override
  @Transactional
  public InventoryTransaction transfer(TransferTransactionCommand command) {
    log.info(
        "inventory transaction transfer start lineCount={} initiatedById={} externalRefLen={} notesLen={}",
        command.lines().size(),
        command.initiatedById(),
        command.externalReference() == null ? 0 : command.externalReference().length(),
        command.notes() == null ? 0 : command.notes().length());

    InventoryTransaction tx = InventoryTransaction.open(
        newTransactionNumber(),
        TransactionType.INTERNAL_TRANSFER,
        command.externalReference(),
        command.notes(),
        command.initiatedById());
    tx = transactionRepository.save(tx);

    for (TransferLine line : command.lines()) {
      assertLocationWritable(line.fromLocationId());
      assertLocationWritable(line.toLocationId());
      Inventory from = getOrCreateInventory(line.itemId(), line.fromLocationId());
      Inventory to = getOrCreateInventory(line.itemId(), line.toLocationId());
      from.removeStock(line.quantity());
      to.addStock(line.quantity());
      inventoryRepository.save(from);
      inventoryRepository.save(to);
      InventoryMovement out = InventoryMovement.recordExit(
          from.getItem(),
          from.getLocation(),
          line.quantity(),
          line.unitCost(),
          InventoryMovement.InventoryMovementType.TRANSFER,
          command.externalReference(),
          "Transfer out",
          command.initiatedById(),
          from.getAvailableQuantity());
      out.setTransactionId(tx.getId());
      movementRepository.save(out);
      InventoryMovement in = InventoryMovement.recordEntry(
          to.getItem(),
          to.getLocation(),
          line.quantity(),
          line.unitCost(),
          InventoryMovement.InventoryMovementType.TRANSFER,
          command.externalReference(),
          "Transfer in",
          command.initiatedById(),
          to.getAvailableQuantity());
      in.setTransactionId(tx.getId());
      movementRepository.save(in);
    }

    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory transaction transfer complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  @Override
  @Transactional
  public InventoryTransaction adjustment(AdjustmentTransactionCommand command) {
    log.info(
        "inventory transaction adjustment start lineCount={} initiatedById={} externalRefLen={} notesLen={}",
        command.lines().size(),
        command.initiatedById(),
        command.externalReference() == null ? 0 : command.externalReference().length(),
        command.notes() == null ? 0 : command.notes().length());

    InventoryTransaction tx = InventoryTransaction.open(
        newTransactionNumber(),
        TransactionType.ADJUSTMENT,
        command.externalReference(),
        command.notes(),
        command.initiatedById());
    tx = transactionRepository.save(tx);

    for (AdjustmentLine line : command.lines()) {
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      int before = inv.getAvailableQuantity();
      inv.adjust(line.newQuantity());
      inventoryRepository.save(inv);
      int delta = line.newQuantity() - before;
      InventoryMovement m;
      if (delta >= 0) {
        m = InventoryMovement.recordEntry(
            inv.getItem(),
            inv.getLocation(),
            Math.abs(delta),
            BigDecimal.ZERO,
            InventoryMovement.InventoryMovementType.ADJUSTMENT_PLUS,
            command.externalReference(),
            line.reason(),
            command.initiatedById(),
            inv.getAvailableQuantity());
      } else {
        m = InventoryMovement.recordExit(
            inv.getItem(),
            inv.getLocation(),
            Math.abs(delta),
            BigDecimal.ZERO,
            InventoryMovement.InventoryMovementType.ADJUSTMENT_MINUS,
            command.externalReference(),
            line.reason(),
            command.initiatedById(),
            inv.getAvailableQuantity());
      }
      if (delta != 0) {
        m.setTransactionId(tx.getId());
        movementRepository.save(m);
      }
    }

    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory transaction adjustment complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  @Override
  @Transactional
  public InventoryTransaction returnFromClient(ReturnClientCommand command) {
    log.info(
        "inventory transaction returnFromClient start lineCount={} initiatedById={} externalRefLen={} notesLen={}",
        command.lines().size(),
        command.initiatedById(),
        command.externalReference() == null ? 0 : command.externalReference().length(),
        command.notes() == null ? 0 : command.notes().length());

    InventoryTransaction tx = InventoryTransaction.open(
        newTransactionNumber(),
        TransactionType.RETURN_FROM_CLIENT,
        command.externalReference(),
        command.notes(),
        command.initiatedById());
    tx = transactionRepository.save(tx);

    for (ReturnClientLine line : command.lines()) {
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      inv.addStock(line.quantity());
      inventoryRepository.save(inv);
      InventoryMovement m = InventoryMovement.recordEntry(
          inv.getItem(),
          inv.getLocation(),
          line.quantity(),
          line.unitCost(),
          InventoryMovement.InventoryMovementType.RETURN_FROM_CLIENT,
          command.externalReference(),
          "Return from client",
          command.initiatedById(),
          inv.getAvailableQuantity());
      m.setTransactionId(tx.getId());
      movementRepository.save(m);
    }

    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory transaction returnFromClient complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  @Override
  @Transactional
  public InventoryTransaction returnToSupplier(ReturnSupplierCommand command) {
    log.info(
        "inventory transaction returnToSupplier start lineCount={} initiatedById={} externalRefLen={} notesLen={}",
        command.lines().size(),
        command.initiatedById(),
        command.externalReference() == null ? 0 : command.externalReference().length(),
        command.notes() == null ? 0 : command.notes().length());

    InventoryTransaction tx = InventoryTransaction.open(
        newTransactionNumber(),
        TransactionType.RETURN_TO_SUPPLIER,
        command.externalReference(),
        command.notes(),
        command.initiatedById());
    tx = transactionRepository.save(tx);

    for (ReturnSupplierLine line : command.lines()) {
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      inv.removeStock(line.quantity());
      inventoryRepository.save(inv);
      InventoryMovement m = InventoryMovement.recordExit(
          inv.getItem(),
          inv.getLocation(),
          line.quantity(),
          line.unitCost(),
          InventoryMovement.InventoryMovementType.RETURN_TO_SUPPLIER,
          command.externalReference(),
          "Return to supplier",
          command.initiatedById(),
          inv.getAvailableQuantity());
      m.setTransactionId(tx.getId());
      movementRepository.save(m);
    }

    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory transaction returnToSupplier complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  @Override
  @Transactional
  public InventoryTransaction scrap(ScrapCommand command) {
    log.info(
        "inventory transaction scrap start lineCount={} initiatedById={} externalRefLen={} notesLen={}",
        command.lines().size(),
        command.initiatedById(),
        command.externalReference() == null ? 0 : command.externalReference().length(),
        command.notes() == null ? 0 : command.notes().length());

    InventoryTransaction tx = InventoryTransaction.open(
        newTransactionNumber(),
        TransactionType.SCRAP_WRITE_OFF,
        command.externalReference(),
        command.notes(),
        command.initiatedById());
    tx = transactionRepository.save(tx);

    for (ScrapLine line : command.lines()) {
      assertLocationWritable(line.locationId());
      Inventory inv = getOrCreateInventory(line.itemId(), line.locationId());
      inv.removeStock(line.quantity());
      inventoryRepository.save(inv);
      InventoryMovement m = InventoryMovement.recordExit(
          inv.getItem(),
          inv.getLocation(),
          line.quantity(),
          line.unitCost(),
          InventoryMovement.InventoryMovementType.SCRAP,
          command.externalReference(),
          "Scrap",
          command.initiatedById(),
          inv.getAvailableQuantity());
      m.setTransactionId(tx.getId());
      movementRepository.save(m);
    }

    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory transaction scrap complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  @Override
  @Transactional
  public InventoryTransaction physicalAdjustment(PhysicalAdjustmentCommand command) {
    log.info(
        "inventory transaction physicalAdjustment start inventoryId={} newQuantity={} performedById={} reasonLen={}",
        command.inventoryId(),
        command.newQuantity(),
        command.performedById(),
        command.reason() == null ? 0 : command.reason().length());

    Inventory inv = inventoryRepository
        .findById(command.inventoryId())
        .orElseThrow(() -> new InventoryNotFoundException(command.inventoryId()));

    InventoryTransaction tx = InventoryTransaction.open(
        newTransactionNumber(),
        TransactionType.ADJUSTMENT,
        null,
        command.reason(),
        command.performedById());
    tx = transactionRepository.save(tx);

    int before = inv.getAvailableQuantity();
    inv.adjust(command.newQuantity());
    inventoryRepository.save(inv);
    int delta = command.newQuantity() - before;
    if (delta != 0) {
      InventoryMovement m;
      if (delta > 0) {
        m = InventoryMovement.recordEntry(
            inv.getItem(),
            inv.getLocation(),
            delta,
            BigDecimal.ZERO,
            InventoryMovement.InventoryMovementType.ADJUSTMENT_PLUS,
            null,
            command.reason(),
            command.performedById(),
            inv.getAvailableQuantity());
      } else {
        m = InventoryMovement.recordExit(
            inv.getItem(),
            inv.getLocation(),
            -delta,
            BigDecimal.ZERO,
            InventoryMovement.InventoryMovementType.ADJUSTMENT_MINUS,
            null,
            command.reason(),
            command.performedById(),
            inv.getAvailableQuantity());
      }
      m.setTransactionId(tx.getId());
      movementRepository.save(m);
    }

    InventoryTransaction completed = finishCompleted(tx);
    log.info(
        "inventory transaction physicalAdjustment complete transactionId={} transactionNumber={}",
        completed.getId(),
        completed.getTransactionNumber());
    return completed;
  }

  @Override
  @Transactional
  public InventoryTransaction submit(Long id) {
    log.info("inventory transaction submit start transactionId={}", id);

    InventoryTransaction tx = getById(id);
    tx.submit();

    InventoryTransaction saved = transactionRepository.save(tx);
    log.info(
        "inventory transaction submit complete transactionId={} transactionNumber={}",
        saved.getId(),
        saved.getTransactionNumber());
    return saved;
  }

  @Override
  @Transactional
  public InventoryTransaction approve(Long id, Long approvedById) {
    log.info("inventory transaction approve start transactionId={} approvedById={}", id, approvedById);

    InventoryTransaction tx = getById(id);
    tx.approve(approvedById);

    InventoryTransaction saved = transactionRepository.save(tx);
    log.info(
        "inventory transaction approve complete transactionId={} transactionNumber={}",
        saved.getId(),
        saved.getTransactionNumber());
    return saved;
  }

  @Override
  @Transactional
  public InventoryTransaction complete(Long id) {
    log.info("inventory transaction markCompleted start transactionId={}", id);

    InventoryTransaction tx = getById(id);
    tx.complete();

    InventoryTransaction saved = transactionRepository.save(tx);
    log.info(
        "inventory transaction markCompleted complete transactionId={} transactionNumber={}",
        saved.getId(),
        saved.getTransactionNumber());
    return saved;
  }

  @Override
  @Transactional
  public void cancel(Long id) {
    log.info("inventory transaction cancel start transactionId={}", id);

    InventoryTransaction tx = getById(id);
    tx.cancel();

    transactionRepository.save(tx);
    log.info("inventory transaction cancel complete transactionId={}", id);
  }
}
