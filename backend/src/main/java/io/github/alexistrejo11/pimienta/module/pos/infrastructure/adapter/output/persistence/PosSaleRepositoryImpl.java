package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosProductReportRow;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosSalesSummary;
import io.github.alexistrejo11.pimienta.module.pos.core.application.PosOpenProductSummary;
import io.github.alexistrejo11.pimienta.module.pos.core.application.query.PosReportFilterQuery;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSaleRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper.PosSalePersistenceMapper;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosSaleSpringDataRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public class PosSaleRepositoryImpl implements PosSaleRepository {

  private final PosSaleSpringDataRepository jpa;

  public PosSaleRepositoryImpl(PosSaleSpringDataRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<PosSale> findBySaleId(UUID saleId) {
    return jpa.findBySaleIdAndDeletedAtIsNull(saleId).map(PosSalePersistenceMapper::toDomain);
  }

  @Override
  public boolean existsBySaleId(UUID saleId) {
    return jpa.existsBySaleIdAndDeletedAtIsNull(saleId);
  }

  @Override
  public PosSale save(PosSale sale) {
    boolean exists = jpa.existsById(sale.getId());
    var entity = PosSalePersistenceMapper.toEntity(sale);
    entity.setNewEntity(!exists);
    return PosSalePersistenceMapper.toDomain(jpa.save(entity));
  }

  @Override
  public Page<PosSale> findAcceptedSales(PosReportFilterQuery filter, Pageable pageable) {
    return jpa.findAcceptedSales(
            filter.headquarterId(),
            filter.from(),
            filter.to(),
            filter.shiftId(),
            filter.productId(),
            filter.lineType(),
            filter.openProductsOnly(),
            PosEventResultStatus.ACCEPTED,
            pageable)
        .map(PosSalePersistenceMapper::toDomain);
  }

  @Override
  public Page<PosProductReportRow> findAcceptedProductTotals(
      PosReportFilterQuery filter, Pageable pageable) {
    List<PosProductReportRow> all =
        jpa.findAcceptedProductTotals(
            filter.headquarterId(),
            filter.from(),
            filter.to(),
            filter.shiftId(),
            filter.productId(),
            filter.lineType(),
            filter.openProductsOnly(),
            PosEventResultStatus.ACCEPTED);
    int start = (int) Math.min(pageable.getOffset(), all.size());
    int end = Math.min(start + pageable.getPageSize(), all.size());
    return new PageImpl<>(all.subList(start, end), pageable, all.size());
  }

  @Override
  public PosSalesSummary summarizeAcceptedSales(long headquarterId, Instant from, Instant to) {
    Object[] row =
        jpa.summarizeAcceptedSales(
            headquarterId, from, to, PosEventResultStatus.ACCEPTED);
    long salesCentavos = row[0] != null ? ((Number) row[0]).longValue() : 0L;
    long ticketCount = row[1] != null ? ((Number) row[1]).longValue() : 0L;
    return new PosSalesSummary(salesCentavos, ticketCount);
  }

  @Override
  public PosOpenProductSummary summarizeOpenProducts(long headquarterId, Instant from, Instant to) {
    Object[] row = jpa.summarizeOpenProducts(headquarterId, from, to, PosEventResultStatus.ACCEPTED);
    return new PosOpenProductSummary(
        number(row[0]), number(row[1]), number(row[2]), number(row[3]));
  }

  private static long number(Object value) {
    return value == null ? 0L : ((Number) value).longValue();
  }
}
