package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import java.util.List;
import java.util.UUID;

public interface PosSyncChangesUseCases {

  ChangesBatch changes(UUID deviceId, String cursor);

  record ChangesBatch(int schemaVersion, String nextCursor, List<ChangeOperation> operations) {}

  record ChangeOperation(String op, String entity, String id, Object data) {
    public static ChangeOperation upsert(String entity, String id, Object data) {
      return new ChangeOperation("upsert", entity, id, data);
    }

    public static ChangeOperation deactivate(String entity, String id) {
      return new ChangeOperation("deactivate", entity, id, null);
    }
  }

  record ProductData(
      String id,
      String sku,
      String barcode,
      String name,
      String saleCategory,
      String unit,
      long priceCentavos,
      long costCentavos,
      boolean available,
      int stockQuantity,
      int stockMinQuantity,
      StockPolicy stockPolicy,
      Integer negativeStockLimit) {}

  record OperatorData(
      String id, String displayName, PosRole role, String pinHash, boolean active) {}

  record PoliciesData(
      boolean allowNegativeStock,
      boolean allowOpenProducts,
      Integer defaultNegativeStockLimit,
      int staleCatalogWarnHours,
      int staleCatalogBlockHours,
      List<String> openAmountCategories,
      boolean stockless) {}
}
