package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.DeviceCreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.ProductRow;
import java.util.UUID;

public interface PosDeviceCatalogUseCases {

  ProductRow createProduct(UUID deviceId, DeviceCreatePosProductCommand command);

  /** Renames the global item and optionally replaces its barcode. SKU stays as stored. */
  ProductRow renameProduct(UUID deviceId, long itemId, String name, String barcode);

  /** Updates sale price and stock policy for this device's headquarter. Name stays on the item. */
  ProductRow updateProductOffer(
      UUID deviceId, long itemId, long salePriceCentavos, StockPolicy stockPolicy);
}
