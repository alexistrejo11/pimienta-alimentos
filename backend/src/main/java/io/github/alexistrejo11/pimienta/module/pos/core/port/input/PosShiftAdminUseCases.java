package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosShiftListItem;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosShiftSalesSummary;
import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosShiftListFilter;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosCashCount;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosCashMovement;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosShiftAdminUseCases {

  Page<PosShiftListItem> list(List<Long> headquarterIds, PosShiftListFilter filter, Pageable pageable);

  ShiftDetail get(UUID shiftId, long headquarterId);

  ShiftReconciliationDetail getReconciliation(UUID shiftId, long headquarterId);

  record ShiftDetail(
      PosShiftListItem listItem, List<PosCashMovement> movements, List<PosCashCount> counts) {}

  record ShiftReconciliationDetail(
      PosShiftListItem listItem,
      List<PosCashMovement> movements,
      List<PosCashCount> counts,
      PosShiftSalesSummary salesSummary) {}
}
