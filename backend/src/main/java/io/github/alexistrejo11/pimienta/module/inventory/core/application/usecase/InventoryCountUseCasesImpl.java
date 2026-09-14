package io.github.alexistrejo11.pimienta.module.inventory.core.application.usecase;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryCountCommands;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryCountSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountResponse;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryCountSession;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement.InventoryMovementType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionType;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryCountInvalidStateException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.InventoryCountNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.ItemNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.exception.StorageLocationNotFoundException;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.InventoryCountUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryCountRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryMovementRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryTransactionRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.StorageLocationRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryCountUseCasesImpl implements InventoryCountUseCases {

  private final InventoryCountRepository counts;
  private final InventoryRepository inventory;
  private final ItemRepository items;
  private final InventoryTransactionRepository transactions;
  private final InventoryMovementRepository movements;
  private final StorageLocationRepository locations;

  public InventoryCountUseCasesImpl(
      InventoryCountRepository counts,
      InventoryRepository inventory,
      ItemRepository items,
      InventoryTransactionRepository transactions,
      InventoryMovementRepository movements,
      StorageLocationRepository locations) {
    this.counts = counts;
    this.inventory = inventory;
    this.items = items;
    this.transactions = transactions;
    this.movements = movements;
    this.locations = locations;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<InventoryCountSession> search(
      InventoryCountSearchCriteria criteria, Pageable pageable) {
    return counts.search(criteria, pageable);
  }

  @Override
  @Transactional(readOnly = true)
  public InventoryCountSession get(long id) {
    return counts.findById(id).orElseThrow(() -> new InventoryCountNotFoundException(id));
  }

  @Override
  @Transactional
  public InventoryCountSession open(InventoryCountCommands.Open command) {
    if (!"FULL".equals(command.type()) && !"PARTIAL".equals(command.type())) {
      throw new IllegalArgumentException("Invalid count type: " + command.type());
    }
    if ("PARTIAL".equals(command.type())
        && (command.itemIds() == null || command.itemIds().isEmpty())) {
      throw new IllegalArgumentException("Partial count requires itemIds");
    }
    if (counts.existsActiveByLocationId(command.locationId())) {
      throw new InventoryCountInvalidStateException(
          0L, "Location already has an active count session");
    }
    locations
        .findById(command.locationId())
        .orElseThrow(() -> new StorageLocationNotFoundException(command.locationId()));

    InventoryCountSession session =
        InventoryCountSession.open(
            command.locationId(),
            InventoryCountSession.CountType.valueOf(command.type()),
            command.createdById());

    var itemIds =
        "FULL".equals(command.type())
            ? inventory.findByLocationId(command.locationId()).stream()
                .map(row -> row.getItem().getId())
                .toList()
            : command.itemIds().stream().distinct().toList();

    for (long itemId : itemIds) {
      items.findById(itemId).orElseThrow(() -> new ItemNotFoundException(itemId));
      int expected =
          inventory
              .findByItemIdAndLocationId(itemId, command.locationId())
              .map(Inventory::getAvailableQuantity)
              .orElse(0);
      session.addResponse(new InventoryCountResponse(itemId, expected));
    }
    return counts.save(session);
  }

  @Override
  @Transactional
  public InventoryCountSession respond(long id, InventoryCountCommands.Response command) {
    InventoryCountSession session = get(id);
    if (session.getStatus() != InventoryCountSession.Status.DRAFT) {
      throw new InventoryCountInvalidStateException(id, "Count is not a draft");
    }
    InventoryCountResponse response =
        session.getResponses().stream()
            .filter(row -> row.getItemId().equals(command.itemId()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalArgumentException("Item is not in count scope: " + command.itemId()));
    if (command.countedQuantity() < 0) {
      throw new IllegalArgumentException("Counted quantity cannot be negative");
    }
    response.setCountedQuantity(command.countedQuantity());
    response.setCountedAt(LocalDateTime.now());
    return counts.save(session);
  }

  @Override
  @Transactional
  public InventoryCountSession submit(long id, long userId) {
    InventoryCountSession session = get(id);
    if (session.getStatus() != InventoryCountSession.Status.DRAFT) {
      throw new InventoryCountInvalidStateException(id, "Count is not a draft");
    }
    if (session.getResponses().stream().anyMatch(row -> row.getCountedQuantity() == null)) {
      throw new InventoryCountInvalidStateException(id, "All count responses are required");
    }
    session
        .getResponses()
        .forEach(
            row -> row.setVariance(row.getCountedQuantity() - row.getExpectedQuantity()));
    session.setStatus(InventoryCountSession.Status.SUBMITTED);
    session.setSubmittedById(userId);
    session.setSubmittedAt(LocalDateTime.now());
    return counts.save(session);
  }

  @Override
  @Transactional
  public InventoryCountSession approve(long id, long userId) {
    InventoryCountSession session = get(id);
    if (session.getStatus() == InventoryCountSession.Status.APPROVED) {
      return session;
    }
    if (session.getStatus() != InventoryCountSession.Status.SUBMITTED) {
      throw new InventoryCountInvalidStateException(id, "Count is not submitted");
    }
    if (userId == session.getCreatedById()) {
      throw new InventoryCountInvalidStateException(id, "Creator cannot approve own count");
    }

    InventoryTransaction tx =
        InventoryTransaction.open(
            "COUNT-" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999),
            TransactionType.PHYSICAL_COUNT,
            null,
            "Physical inventory count " + id,
            userId);
    tx = transactions.save(tx);

    for (InventoryCountResponse response : session.getResponses()) {
      if (response.getVariance() == 0) {
        continue;
      }
      Inventory inv =
          inventory
              .findByItemIdAndLocationId(response.getItemId(), session.getLocationId())
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Inventory row not found for item " + response.getItemId()));
      int current = inv.getAvailableQuantity();
      int target = current + response.getVariance();
      if (target < 0) {
        throw new InventoryCountInvalidStateException(
            id, "Count variance would make stock negative");
      }
      if (response.getVariance() > 0) {
        inv.addStock(response.getVariance());
      } else {
        inv.removeStock(-response.getVariance());
      }
      inventory.save(inv);

      InventoryMovement movement =
          response.getVariance() > 0
              ? InventoryMovement.recordEntry(
                  inv.getItem(),
                  inv.getLocation(),
                  response.getVariance(),
                  BigDecimal.ZERO,
                  InventoryMovementType.ADJUSTMENT_PLUS,
                  "COUNT-" + id,
                  "Physical count variance",
                  userId,
                  inv.getAvailableQuantity())
              : InventoryMovement.recordExit(
                  inv.getItem(),
                  inv.getLocation(),
                  -response.getVariance(),
                  BigDecimal.ZERO,
                  InventoryMovementType.ADJUSTMENT_MINUS,
                  "COUNT-" + id,
                  "Physical count variance",
                  userId,
                  inv.getAvailableQuantity());
      movement.setTransactionId(tx.getId());
      movements.save(movement);
    }

    tx.complete();
    transactions.save(tx);
    session.setStatus(InventoryCountSession.Status.APPROVED);
    session.setApprovedById(userId);
    session.setApprovedAt(LocalDateTime.now());
    return counts.save(session);
  }

  @Override
  @Transactional
  public void cancel(long id) {
    InventoryCountSession session = get(id);
    if (session.getStatus() == InventoryCountSession.Status.APPROVED) {
      throw new InventoryCountInvalidStateException(id, "Approved count cannot be cancelled");
    }
    session.setStatus(InventoryCountSession.Status.CANCELLED);
    session.setCancelledAt(LocalDateTime.now());
    counts.save(session);
  }
}
