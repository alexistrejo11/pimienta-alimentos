package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosCashCount;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosCashMovement;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosShift;
import java.util.List; import java.util.UUID;
import org.springframework.data.domain.Page; import org.springframework.data.domain.Pageable;

public interface PosShiftAdminUseCases {
  Page<PosShift> list(List<Long> headquarterIds, Pageable pageable);
  ShiftDetail get(UUID shiftId, long headquarterId);
  record ShiftDetail(PosShift shift, List<PosCashMovement> movements, List<PosCashCount> counts) {}
}
