package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemCostChangePort;
import io.github.alexistrejo11.pimienta.module.product.core.port.output.ProductRepository;
import org.springframework.stereotype.Component;

/** Writes one catalog change per sede so tablets pull the new unit cost. */
@Component
public class PosItemCostChangeAdapter implements ItemCostChangePort {

  private final ProductRepository productRepository;
  private final HeadquarterItemRepository headquarterItemRepository;
  private final PosChangeLogService posChangeLogService;

  public PosItemCostChangeAdapter(
      ProductRepository productRepository,
      HeadquarterItemRepository headquarterItemRepository,
      PosChangeLogService posChangeLogService) {
    this.productRepository = productRepository;
    this.headquarterItemRepository = headquarterItemRepository;
    this.posChangeLogService = posChangeLogService;
  }

  @Override
  public void publish(long inventoryItemId) {
    productRepository
        .findByInventoryItemId(inventoryItemId)
        .ifPresent(
            product ->
                posChangeLogService.appendCatalogItemsForGlobalItem(
                    product.getId(), headquarterItemRepository.findAllByProductId(product.getId())));
  }
}
