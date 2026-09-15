package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosShiftListFilter;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosCashCount;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosCashMovement;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosShift;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosShiftRepository {
  void materialize(PosSyncEvent event);

  Page<PosShift> findByHeadquarterIds(
      java.util.List<Long> headquarterIds, PosShiftListFilter filter, Pageable pageable);

  Optional<PosShift> findById(UUID shiftId, long headquarterId);

  java.util.List<PosCashMovement> movements(UUID shiftId);

  java.util.List<PosCashCount> counts(UUID shiftId);
}
