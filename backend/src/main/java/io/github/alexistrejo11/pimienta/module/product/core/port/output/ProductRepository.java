package io.github.alexistrejo11.pimienta.module.product.core.port.output;

import io.github.alexistrejo11.pimienta.module.product.core.application.query.ProductSearchCriteria;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {

  Optional<Product> findById(long id);

  Optional<Product> findByInventoryItemId(long inventoryItemId);

  Optional<Product> findBySkuOrBarcode(String skuOrBarcode);

  Page<Product> search(ProductSearchCriteria criteria, Pageable pageable);

  List<Product> findPosCandidates(long headquarterId);

  Product save(Product product);

  boolean existsByBarcodeIgnoreCaseExcludingId(String barcode, Long excludeId);
}
