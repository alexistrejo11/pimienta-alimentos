package io.github.alexistrejo11.pimienta.module.headquarter.core.port.input;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertHeadquarterItemCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;

public interface HeadquarterPosCatalogUseCases {

  Page<HeadquarterItem> list(long headquarterId, Pageable pageable);
  Page<HeadquarterItem> search(long headquarterId, String search, String saleCategory,
      Boolean available, StockPolicy stockPolicy, Pageable pageable);

  HeadquarterItem get(long headquarterId, long itemId);

  HeadquarterItem upsert(long headquarterId, long itemId, UpsertHeadquarterItemCommand command);
  List<Item> candidates(long headquarterId);

  HeadquarterItem softDelete(long headquarterId, long itemId);
}
