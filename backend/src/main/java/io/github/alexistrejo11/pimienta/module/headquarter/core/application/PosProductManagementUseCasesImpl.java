package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreatePosProductCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.exception.PosSaleCategoryNotFoundException;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.PosSaleCategoryRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PosProductManagementUseCasesImpl implements PosProductManagementUseCases {
  private final HeadquarterRepository headquarters;
  private final ItemRepository items;
  private final HeadquarterItemRepository catalog;
  private final PosSaleCategoryRepository categories;

  public PosProductManagementUseCasesImpl(HeadquarterRepository headquarters, ItemRepository items,
      HeadquarterItemRepository catalog, PosSaleCategoryRepository categories) {
    this.headquarters = headquarters; this.items = items; this.catalog = catalog; this.categories = categories;
  }

  @Override @Transactional
  public CreatedPosProduct create(long headquarterId, CreatePosProductCommand c) {
    headquarters.findById(headquarterId).orElseThrow();
    var category = categories.findById(headquarterId, c.posSaleCategoryId())
        .filter(v -> v.isActive()).orElseThrow(() -> new PosSaleCategoryNotFoundException(c.posSaleCategoryId()));
    String sku = items.nextInternalSku();
    Item item = Item.create(sku, c.name(), c.description(), c.costPrice(),
        c.category() != null ? c.category() : ItemCategory.FINISHED_GOOD, c.unit(), c.reorderPoint(), c.reorderQuantity());
    item.setBrand(c.brand()); item.setBarcode(blankToNull(c.barcode()));
    Item saved = items.save(item);
    HeadquarterItem row = HeadquarterItem.builder().withHeadquarterId(headquarterId).withItemId(saved.getId())
        .withPosSaleCategoryId(category.getId()).withSaleCategory(category.getName())
        .withSalePrice(c.salePrice()).withAvailable(c.available()).withStockPolicy(c.stockPolicy() != null ? c.stockPolicy() : StockPolicy.CONTROLLED)
        .withNegativeStockLimit(c.negativeStockLimit()).register();
    return new CreatedPosProduct(saved, catalog.save(row));
  }

  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
