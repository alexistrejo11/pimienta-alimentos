package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosOperatorRepository {

  Optional<PosOperator> findById(long id);

  Page<PosOperator> findAll(Pageable pageable);

  Page<PosOperator> findByHeadquarterId(long headquarterId, Pageable pageable);

  List<PosOperator> findByHeadquarterId(long headquarterId);

  /** Soft-deleted operators that still list the headquarter (for deactivate deltas). */
  List<PosOperator> findDeletedByHeadquarterIdUpdatedAfter(
      long headquarterId, LocalDateTime since);

  PosOperator save(PosOperator operator);
}
