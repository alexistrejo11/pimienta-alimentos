package io.github.alexistrejo11.pimienta.module.headquarter.core.port.output;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HeadquarterItemRepository {

  Optional<HeadquarterItem> findByHeadquarterIdAndItemId(long headquarterId, long itemId);

  Page<HeadquarterItem> findByHeadquarterId(long headquarterId, Pageable pageable);

  List<HeadquarterItem> findAllByHeadquarterId(long headquarterId);

  List<HeadquarterItem> findAllByItemId(long itemId);

  /** Soft-deleted catalog rows for POS deactivate deltas. */
  List<HeadquarterItem> findDeletedByHeadquarterIdAndDeletedAtAfter(
      long headquarterId, LocalDateTime since);

  HeadquarterItem save(HeadquarterItem item);
}
