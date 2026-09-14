package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosDeviceStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosRole;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PosSyncBootstrapUseCases {

  BootstrapSnapshot bootstrap(UUID deviceId);

  record BootstrapSnapshot(
      int schemaVersion,
      String kind,
      UUID snapshotId,
      Instant generatedAt,
      SiteRow site,
      DeviceRow device,
      List<OperatorRow> operators,
      List<ProductRow> products,
      List<String> openAmountCategories,
      Policies policies,
      Cursors cursors) {}

  record SiteRow(String id, String name, String address, String currency) {}

  record DeviceRow(String id, String name, String visibleCode, PosDeviceStatus status) {}

  record OperatorRow(
      String id, String displayName, PosRole role, String pinHash, boolean active) {}

  record ProductRow(
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

  record Policies(
      boolean allowNegativeStock,
      boolean allowOpenProducts,
      Integer defaultNegativeStockLimit,
      int staleCatalogWarnHours,
      int staleCatalogBlockHours) {}

  record Cursors(String changes) {}
}
