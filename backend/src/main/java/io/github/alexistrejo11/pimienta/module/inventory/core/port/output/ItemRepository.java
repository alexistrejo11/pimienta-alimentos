package io.github.alexistrejo11.pimienta.module.inventory.core.port.output;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.ItemSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemRepository {

  Optional<Item> findById(long id);

  Optional<Item> findBySkuOrBarcode(String skuOrBarcode);

  Page<Item> search(ItemSearchCriteria criteria, Pageable pageable);

  Item save(Item item);

  String nextInternalSku();

  boolean existsBySkuIgnoreCaseExcludingId(String sku, Long excludeId);

  /** Global barcode uniqueness including soft-deleted rows (D5). */
  boolean existsByBarcodeIgnoreCaseExcludingId(String barcode, Long excludeId);
}
