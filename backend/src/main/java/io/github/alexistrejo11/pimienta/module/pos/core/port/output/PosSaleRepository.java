package io.github.alexistrejo11.pimienta.module.pos.core.port.output;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosProductReportRow;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosShiftSalesSummary;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosSalesSummary;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosOpenProductSummary;
import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PosSaleRepository {

  Optional<PosSale> findBySaleId(UUID saleId);

  boolean existsBySaleId(UUID saleId);

  PosSale save(PosSale sale);

  Page<PosSale> findAcceptedSales(PosReportFilterQuery filter, Pageable pageable);

  Page<PosProductReportRow> findAcceptedProductTotals(PosReportFilterQuery filter, Pageable pageable);

  PosSalesSummary summarizeAcceptedSales(long headquarterId, Instant from, Instant to);

  PosOpenProductSummary summarizeOpenProducts(long headquarterId, Instant from, Instant to);

  PosShiftSalesSummary summarizeAcceptedSalesForShift(UUID shiftId);
}
