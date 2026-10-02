package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductIdentityChangePort;
import org.springframework.stereotype.Component;

/** Writes one catalog change per sede so tablets pull the new name and barcode. */
@Component
public class PosProductIdentityChangeAdapter implements ProductIdentityChangePort {

  private final HeadquarterItemRepository headquarterItemRepository;
  private final PosChangeLogService posChangeLogService;

  public PosProductIdentityChangeAdapter(
      HeadquarterItemRepository headquarterItemRepository, PosChangeLogService posChangeLogService) {
    this.headquarterItemRepository = headquarterItemRepository;
    this.posChangeLogService = posChangeLogService;
  }

  @Override
  public void publish(long productId) {
    posChangeLogService.appendCatalogItemsForGlobalItem(
        productId, headquarterItemRepository.findAllByProductId(productId));
  }
}
