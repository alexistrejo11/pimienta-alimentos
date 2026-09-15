package io.github.alexistrejo11.pimienta.module.pos.core.port.input;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosProductReportRow;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosReportSummaryRow;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosSaleReportRow;
import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosAdminReportUseCases {

  Page<PosSaleReportRow> sales(PosReportFilterQuery filter, Pageable pageable);

  Page<PosProductReportRow> products(PosReportFilterQuery filter, Pageable pageable);

  Page<PosSyncEvent> wasteCancellations(PosReportFilterQuery filter, Pageable pageable);

  Page<PosSyncEvent> shiftCloses(PosReportFilterQuery filter, Pageable pageable);

  List<PosReportSummaryRow> summary(List<Long> headquarterIds, Instant from, Instant to);
}
