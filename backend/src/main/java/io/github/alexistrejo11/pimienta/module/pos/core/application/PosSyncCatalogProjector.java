package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.StorageLocation;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosLocationUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.OperatorRow;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.Policies;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.ProductRow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Shared POS master projection (bootstrap + changes). */
@Component
public class PosSyncCatalogProjector {

  private final ItemRepository itemRepository;
  private final PosLocationUseCases posLocationUseCases;
  private final InventoryRepository inventoryRepository;

  public PosSyncCatalogProjector(
      ItemRepository itemRepository,
      PosLocationUseCases posLocationUseCases,
      InventoryRepository inventoryRepository) {
    this.itemRepository = itemRepository;
    this.posLocationUseCases = posLocationUseCases;
    this.inventoryRepository = inventoryRepository;
  }

  public Optional<ProductRow> toProductRow(HeadquarterItem row) {
    Optional<Item> itemOpt = itemRepository.findById(row.getItemId());
    if (itemOpt.isEmpty()) {
      return Optional.empty();
    }
    Item item = itemOpt.get();
    int stockQty = stockQuantity(row.getHeadquarterId(), item.getId());
    return Optional.of(
        new ProductRow(
            String.valueOf(item.getId()),
            item.getSku(),
            item.getBarcode(),
            item.getName(),
            row.getSaleCategory(),
            item.getUnit() != null ? item.getUnit().name() : "PIECE",
            toCentavos(row.getSalePrice()),
            toCentavos(item.getCostPrice()),
            row.isAvailable(),
            stockQty,
            item.getReorderPoint(),
            row.getStockPolicy(),
            row.getNegativeStockLimit()));
  }

  public OperatorRow toOperatorRow(PosOperator op) {
    return new OperatorRow(
        String.valueOf(op.getId()),
        op.getDisplayName(),
        op.getPosRole(),
        op.getPinHash(),
        op.isActive());
  }

  public Policies toPolicies(PosOperationalConfig config) {
    Integer defaultNegative = config != null ? config.getDefaultNegativeStockLimit() : null;
    int warnHours = config != null ? config.getCatalogStaleWarnHours() : 24;
    int blockHours = config != null ? config.getCatalogStaleBlockHours() : 72;
    return new Policies(
        true,
        config != null && config.isAllowOpenProducts(),
        defaultNegative,
        warnHours,
        blockHours,
        config != null && config.isStockless());
  }

  public int stockQuantity(long headquarterId, long itemId) {
    Optional<StorageLocation> posLocation = posLocationUseCases.findPosLocation(headquarterId);
    if (posLocation.isEmpty()) {
      return 0;
    }
    return inventoryRepository
        .findByItemIdAndLocationId(itemId, posLocation.get().getId())
        .map(Inventory::getAvailableQuantity)
        .orElse(0);
  }

  public Optional<Inventory> findPosInventory(long headquarterId, long itemId) {
    return posLocationUseCases
        .findPosLocation(headquarterId)
        .flatMap(loc -> inventoryRepository.findByItemIdAndLocationId(itemId, loc.getId()));
  }

  public Optional<Item> findItem(long itemId) {
    return itemRepository.findById(itemId);
  }

  static long toCentavos(BigDecimal amount) {
    if (amount == null) {
      return 0L;
    }
    return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
  }
}
