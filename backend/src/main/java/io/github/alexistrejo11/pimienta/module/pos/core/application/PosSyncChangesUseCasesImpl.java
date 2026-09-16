package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosOperationalConfigRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceRevokedException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosSyncCursorInvalidException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.OperatorRow;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.Policies;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.ProductRow;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosChangeLogRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosOperatorRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosSyncChangesUseCasesImpl implements PosSyncChangesUseCases {

  private static final int SCHEMA_VERSION = 1;
  private static final int MAX_BATCH_SIZE = 500;

  private final PosDeviceRepository deviceRepository;
  private final HeadquarterItemRepository headquarterItemRepository;
  private final PosOperatorRepository operatorRepository;
  private final PosOperationalConfigRepository posOperationalConfigRepository;
  private final PosSyncCatalogProjector projector;
  private final PosChangeLogRepository changeLogRepository;

  public PosSyncChangesUseCasesImpl(
      PosDeviceRepository deviceRepository,
      HeadquarterItemRepository headquarterItemRepository,
      PosOperatorRepository operatorRepository,
      PosOperationalConfigRepository posOperationalConfigRepository,
      PosSyncCatalogProjector projector,
      PosChangeLogRepository changeLogRepository) {
    this.deviceRepository = deviceRepository;
    this.headquarterItemRepository = headquarterItemRepository;
    this.operatorRepository = operatorRepository;
    this.posOperationalConfigRepository = posOperationalConfigRepository;
    this.projector = projector;
    this.changeLogRepository = changeLogRepository;
  }

  @Override
  @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
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
    long upperBound =
        changeLogRepository.findLastByHeadquarterId(hqId).map(e -> e.sequence()).orElse(0L);
    long firstSequence =
        changeLogRepository.findFirstByHeadquarterId(hqId).map(e -> e.sequence()).orElse(0L);
    if (cursor.sequence() > upperBound
        || (firstSequence > 0 && cursor.sequence() < firstSequence - 1)) {
      throw new PosSyncCursorInvalidException(cursorRaw);
    }

    List<PosChangeLogRepository.PosChangeLogEntry> entries =
        changeLogRepository.findAfterSequence(hqId, cursor.sequence(), upperBound, MAX_BATCH_SIZE);
    List<ChangeOperation> operations = entries.stream().map(entry -> project(hqId, entry)).toList();
    long nextSequence = entries.isEmpty() ? cursor.sequence() : entries.get(entries.size() - 1).sequence();
    return new ChangesBatch(
        SCHEMA_VERSION, PosSyncCursor.of(hqId, nextSequence).format(), operations);
  }

  private ChangeOperation project(long hqId, PosChangeLogRepository.PosChangeLogEntry entry) {
    boolean deactivate = "DEACTIVATE".equals(entry.operation());
    if ("OPERATOR".equals(entry.entityType())) {
      if (deactivate) return ChangeOperation.deactivate("operator", entry.entityId());
      Optional<PosOperator> operator = operatorRepository.findById(Long.parseLong(entry.entityId()));
      if (operator.isEmpty() || !operator.get().getHeadquarterIds().contains(hqId)) {
        return ChangeOperation.deactivate("operator", entry.entityId());
      }
      OperatorRow row = projector.toOperatorRow(operator.get());
      return ChangeOperation.upsert(
          "operator", row.id(), new OperatorData(row.id(), row.displayName(), row.role(), row.pinHash(), row.active()));
    }
    if ("POLICY".equals(entry.entityType())) {
      PosOperationalConfig config = posOperationalConfigRepository.findByHeadquarterId(hqId).orElse(null);
      if (config == null) return ChangeOperation.deactivate("policies", entry.entityId());
      Policies policies = projector.toPolicies(config);
      return ChangeOperation.upsert(
          "policies", entry.entityId(), new PoliciesData(policies.allowNegativeStock(), policies.allowOpenProducts(),
              policies.defaultNegativeStockLimit(), policies.staleCatalogWarnHours(), policies.staleCatalogBlockHours(),
              config.getOpenAmountCategories()));
    }
    long itemId = Long.parseLong(entry.entityId());
    if (deactivate) return ChangeOperation.deactivate("product", entry.entityId());
    return headquarterItemRepository.findByHeadquarterIdAndItemId(hqId, itemId)
        .flatMap(projector::toProductRow)
        .map(row -> ChangeOperation.upsert("product", row.id(), toProductData(row)))
        .orElseGet(() -> ChangeOperation.deactivate("product", entry.entityId()));
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

}
