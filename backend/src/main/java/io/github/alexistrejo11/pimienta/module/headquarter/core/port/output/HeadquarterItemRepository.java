package io.github.alexistrejo11.pimienta.module.headquarter.core.port.output;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem.StockPolicy;

public interface HeadquarterItemRepository {

  Optional<HeadquarterItem> findByHeadquarterIdAndProductId(long headquarterId, long productId);

  Page<HeadquarterItem> findByHeadquarterId(long headquarterId, Pageable pageable);

  Page<HeadquarterItem> search(
      long headquarterId,
      String search,
      String saleCategory,
      Boolean available,
      StockPolicy stockPolicy,
      Boolean hasBarcode,
      Pageable pageable);

  List<HeadquarterItem> findAllByHeadquarterId(long headquarterId);

  List<HeadquarterItem> findAllByProductId(long productId);

  boolean existsControlledByProductId(long productId);

  /** Soft-deleted catalog rows for POS deactivate deltas. */
  List<HeadquarterItem> findDeletedByHeadquarterIdAndDeletedAtAfter(
      long headquarterId, LocalDateTime since);

  HeadquarterItem save(HeadquarterItem item);
}
