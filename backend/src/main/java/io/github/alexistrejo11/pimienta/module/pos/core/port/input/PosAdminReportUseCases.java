package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosProductReportRow;
import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosAdminReportUseCases {

  Page<PosSale> sales(PosReportFilterQuery filter, Pageable pageable);

  Page<PosProductReportRow> products(PosReportFilterQuery filter, Pageable pageable);

  Page<PosSyncEvent> wasteCancellations(PosReportFilterQuery filter, Pageable pageable);

  Page<PosSyncEvent> shiftCloses(PosReportFilterQuery filter, Pageable pageable);
}
