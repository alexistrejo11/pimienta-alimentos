package io.github.alexistrejo11.pimienta.module.product.core.port.input;

import io.github.alexistrejo11.pimienta.module.product.core.application.command.CreateProductCommand;
import io.github.alexistrejo11.pimienta.module.product.core.application.command.UpdateProductCommand;
import io.github.alexistrejo11.pimienta.module.product.core.application.query.ProductSearchCriteria;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductManagementUseCases {

  Page<Product> search(ProductSearchCriteria criteria, Pageable pageable);

  Product getById(long id);

  Product getBySkuOrBarcode(String skuOrBarcode);

  Product create(CreateProductCommand command);

  Product update(long id, UpdateProductCommand command);

  List<Product> findPosCandidates(long headquarterId);
}
