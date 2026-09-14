package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosOperationalConfigRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncTombstone;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceRevokedException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosSyncCursorInvalidException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.OperatorRow;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.Policies;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.ProductRow;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosOperatorRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncTombstoneRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosSyncChangesUseCasesImpl implements PosSyncChangesUseCases {

  private static final int SCHEMA_VERSION = 1;

  private final PosDeviceRepository deviceRepository;
  private final HeadquarterItemRepository headquarterItemRepository;
  private final PosOperatorRepository operatorRepository;
  private final PosOperationalConfigRepository posOperationalConfigRepository;
  private final PosSyncTombstoneRepository tombstoneRepository;
  private final PosSyncCatalogProjector projector;

  public PosSyncChangesUseCasesImpl(
      PosDeviceRepository deviceRepository,
      HeadquarterItemRepository headquarterItemRepository,
      PosOperatorRepository operatorRepository,
      PosOperationalConfigRepository posOperationalConfigRepository,
      PosSyncTombstoneRepository tombstoneRepository,
      PosSyncCatalogProjector projector) {
    this.deviceRepository = deviceRepository;
    this.headquarterItemRepository = headquarterItemRepository;
    this.operatorRepository = operatorRepository;
    this.posOperationalConfigRepository = posOperationalConfigRepository;
    this.tombstoneRepository = tombstoneRepository;
    this.projector = projector;
  }

  @Override
  @Transactional(readOnly = true)
  public ChangesBatch changes(UUID deviceId, String cursorRaw) {
    PosDevice device =
        deviceRepository.findById(deviceId).orElseThrow(() -> new PosDeviceNotFoundException(deviceId));
    if (device.isRevoked()) {
      throw new PosDeviceRevokedException(deviceId);
    }

    PosSyncCursor cursor =
        PosSyncCursor.tryParse(cursorRaw)
            .orElseThrow(() -> new PosSyncCursorInvalidException(cursorRaw));
    if (cursor.headquarterId() != device.getHeadquarterId()) {
      throw new PosSyncCursorInvalidException(cursorRaw);
    }

    long hqId = device.getHeadquarterId();
    LocalDateTime since = cursor.since();
    long nextWatermark = Math.max(cursor.watermarkMillis(), Instant.now().toEpochMilli());
    List<ChangeOperation> operations = new ArrayList<>();
    Set<String> deactivatedProductIds = new HashSet<>();
    Set<String> deactivatedOperatorIds = new HashSet<>();

    for (HeadquarterItem deleted :
        headquarterItemRepository.findDeletedByHeadquarterIdAndDeletedAtAfter(hqId, since)) {
      String id = String.valueOf(deleted.getItemId());
      deactivatedProductIds.add(id);
      operations.add(ChangeOperation.deactivate(PosSyncTombstone.ENTITY_PRODUCT, id));
      nextWatermark = Math.max(nextWatermark, PosSyncCursor.toMillis(deleted.getDeletedAt()));
    }

    for (PosOperator deletedOp :
        operatorRepository.findDeletedByHeadquarterIdUpdatedAfter(hqId, since)) {
      String id = String.valueOf(deletedOp.getId());
      deactivatedOperatorIds.add(id);
      operations.add(ChangeOperation.deactivate(PosSyncTombstone.ENTITY_OPERATOR, id));
      nextWatermark = Math.max(nextWatermark, PosSyncCursor.toMillis(deletedOp.getUpdatedAt()));
    }

    for (PosSyncTombstone tombstone :
        tombstoneRepository.findByHeadquarterIdAndCreatedAtAfter(hqId, since)) {
      if (PosSyncTombstone.ENTITY_PRODUCT.equals(tombstone.entity())) {
        if (deactivatedProductIds.add(tombstone.entityId())) {
          operations.add(
              ChangeOperation.deactivate(PosSyncTombstone.ENTITY_PRODUCT, tombstone.entityId()));
        }
      } else if (PosSyncTombstone.ENTITY_OPERATOR.equals(tombstone.entity())) {
        if (deactivatedOperatorIds.add(tombstone.entityId())) {
          operations.add(
              ChangeOperation.deactivate(PosSyncTombstone.ENTITY_OPERATOR, tombstone.entityId()));
        }
      }
      nextWatermark = Math.max(nextWatermark, PosSyncCursor.toMillis(tombstone.createdAt()));
    }

    for (HeadquarterItem row : headquarterItemRepository.findAllByHeadquarterId(hqId)) {
      boolean hiChanged = isAfter(row.getUpdatedAt(), since);
      Optional<Item> itemOpt = projector.findItem(row.getItemId());
      boolean itemChanged =
          itemOpt.map(item -> isAfter(item.getUpdatedAt(), since)).orElse(false);
      Optional<Inventory> invOpt = projector.findPosInventory(hqId, row.getItemId());
      boolean stockChanged =
          invOpt.map(inv -> isAfter(inv.getUpdatedAt(), since)).orElse(false);
      if (!hiChanged && !itemChanged && !stockChanged) {
        continue;
      }
      Optional<ProductRow> productOpt = projector.toProductRow(row);
      if (productOpt.isEmpty()) {
        continue;
      }
      ProductRow product = productOpt.get();
      if (deactivatedProductIds.contains(product.id())) {
        continue;
      }
      operations.add(
          ChangeOperation.upsert(
              PosSyncTombstone.ENTITY_PRODUCT, product.id(), toProductData(product)));
      nextWatermark =
          Math.max(
              nextWatermark,
              maxMillis(
                  row.getUpdatedAt(),
                  itemOpt.map(Item::getUpdatedAt).orElse(null),
                  invOpt.map(Inventory::getUpdatedAt).orElse(null)));
    }

    for (PosOperator op : operatorRepository.findByHeadquarterId(hqId)) {
      if (!isAfter(op.getUpdatedAt(), since)
          || deactivatedOperatorIds.contains(String.valueOf(op.getId()))) {
        continue;
      }
      OperatorRow row = projector.toOperatorRow(op);
      operations.add(
          ChangeOperation.upsert(
              PosSyncTombstone.ENTITY_OPERATOR,
              row.id(),
              new OperatorData(
                  row.id(), row.displayName(), row.role(), row.pinHash(), row.active())));
      nextWatermark = Math.max(nextWatermark, PosSyncCursor.toMillis(op.getUpdatedAt()));
    }

    PosOperationalConfig config =
        posOperationalConfigRepository.findByHeadquarterId(hqId).orElse(null);
    if (config != null && isAfter(config.getUpdatedAt(), since)) {
      Policies policies = projector.toPolicies(config);
      operations.add(
          ChangeOperation.upsert(
              "policies",
              String.valueOf(hqId),
              new PoliciesData(
                   policies.allowNegativeStock(),
                   policies.allowOpenProducts(),
                  policies.defaultNegativeStockLimit(),
                  policies.staleCatalogWarnHours(),
                  policies.staleCatalogBlockHours(),
                  config.getOpenAmountCategories())));
      nextWatermark = Math.max(nextWatermark, PosSyncCursor.toMillis(config.getUpdatedAt()));
    }

    PosSyncCursor next = PosSyncCursor.of(hqId, nextWatermark);
    return new ChangesBatch(SCHEMA_VERSION, next.format(), List.copyOf(operations));
  }

  private static ProductData toProductData(ProductRow product) {
    return new ProductData(
        product.id(),
        product.sku(),
        product.barcode(),
        product.name(),
        product.saleCategory(),
        product.unit(),
        product.priceCentavos(),
        product.costCentavos(),
        product.available(),
        product.stockQuantity(),
        product.stockMinQuantity(),
        product.stockPolicy(),
        product.negativeStockLimit());
  }

  private static boolean isAfter(LocalDateTime value, LocalDateTime since) {
    return value != null && value.isAfter(since);
  }

  private static long maxMillis(LocalDateTime a, LocalDateTime b, LocalDateTime c) {
    long max = 0L;
    max = Math.max(max, PosSyncCursor.toMillis(a));
    max = Math.max(max, PosSyncCursor.toMillis(b));
    max = Math.max(max, PosSyncCursor.toMillis(c));
    return max;
  }
}
