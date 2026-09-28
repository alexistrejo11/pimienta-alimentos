package io.github.alexistrejo11.pimienta.module.headquarter.core.port.input;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertHeadquarterItemCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HeadquarterPosCatalogUseCases {

  Page<HeadquarterItem> list(long headquarterId, Pageable pageable);

  Page<HeadquarterItem> search(
      long headquarterId,
      String search,
      String saleCategory,
      Boolean available,
      StockPolicy stockPolicy,
      Pageable pageable);

  HeadquarterItem get(long headquarterId, long productId);

  HeadquarterItem upsert(long headquarterId, long productId, UpsertHeadquarterItemCommand command);

  List<Product> candidates(long headquarterId);

  HeadquarterItem softDelete(long headquarterId, long productId);
}
