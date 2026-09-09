package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.mapper;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosPaymentMethod;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStockPolicy;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSaleJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSaleLineJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosSalePaymentJpaEntity;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PosSalePersistenceMapper {

  private PosSalePersistenceMapper() {}

  public static PosSale toDomain(PosSaleJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    List<PosSale.Line> lines = new ArrayList<>();
    if (entity.getLines() != null) {
      for (PosSaleLineJpaEntity line : entity.getLines()) {
        lines.add(
            new PosSale.Line(
                line.getLineId(),
                line.getProductId(),
                line.getProductName(),
                line.getSaleCategory(),
                line.getQuantity(),
                line.getUnit(),
                line.getUnitPriceCentavos(),
                line.getSubtotalCentavos(),
                line.getStockPolicy(),
                line.isSoldWithNegativeStock(),
                line.isSoldWhileUnavailable(),
                line.getRawBarcode()));
      }
    }
    List<PosSale.Payment> payments = new ArrayList<>();
    if (entity.getPayments() != null) {
      for (PosSalePaymentJpaEntity payment : entity.getPayments()) {
        payments.add(
            new PosSale.Payment(
                payment.getPaymentId(),
                payment.getMethod(),
                payment.getAmountCentavos(),
                payment.getTenderedCentavos(),
                payment.getChangeCentavos()));
      }
    }
    return PosSale.builder()
        .withId(entity.getSaleId())
        .withEventId(entity.getEventId())
        .withHeadquarterId(entity.getHeadquarterId())
        .withDeviceId(entity.getDeviceId())
        .withShiftId(entity.getShiftId())
        .withCashierOperatorId(entity.getCashierOperatorId())
        .withFolio(entity.getFolio())
        .withGrossCentavos(entity.getGrossCentavos())
        .withDiscountCentavos(entity.getDiscountCentavos())
        .withTotalCentavos(entity.getTotalCentavos())
        .withStatus(entity.getStatus())
        .withOccurredAt(entity.getOccurredAt())
        .withLines(lines)
        .withPayments(payments)
        .withCreatedAt(entity.getCreatedAt())
        .withUpdatedAt(entity.getUpdatedAt())
        .withDeletedAt(entity.getDeletedAt())
        .withVersion(entity.getVersion())
        .reconstruct();
  }

  public static PosSaleJpaEntity toEntity(PosSale domain) {
    PosSaleJpaEntity e = new PosSaleJpaEntity();
    e.setSaleId(domain.getId());
    e.setEventId(domain.getEventId());
    e.setHeadquarterId(domain.getHeadquarterId());
    e.setDeviceId(domain.getDeviceId());
    e.setShiftId(domain.getShiftId());
    e.setCashierOperatorId(domain.getCashierOperatorId());
    e.setFolio(
        domain.getFolio() != null && !domain.getFolio().isBlank() ? domain.getFolio() : "");
    e.setGrossCentavos(domain.getGrossCentavos());
    e.setDiscountCentavos(domain.getDiscountCentavos());
    e.setTotalCentavos(domain.getTotalCentavos());
    e.setStatus(domain.getStatus() != null ? domain.getStatus() : PosSaleStatus.CONFIRMED);
    e.setOccurredAt(domain.getOccurredAt() != null ? domain.getOccurredAt() : Instant.now());
    e.setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt() : LocalDateTime.now());
    e.setUpdatedAt(domain.getUpdatedAt() != null ? domain.getUpdatedAt() : LocalDateTime.now());
    e.setDeletedAt(domain.getDeletedAt());
    e.setVersion(domain.getVersion());

    LocalDateTime now = LocalDateTime.now();
    List<PosSaleLineJpaEntity> lineEntities = new ArrayList<>();
    for (PosSale.Line line : domain.getLines()) {
      PosSaleLineJpaEntity le = new PosSaleLineJpaEntity();
      le.setSale(e);
      le.setLineId(line.lineId());
      le.setProductId(line.productId());
      le.setProductName(line.productName() != null ? line.productName() : "");
      le.setSaleCategory(blankToNull(line.saleCategory()));
      le.setQuantity(line.quantity());
      le.setUnit(line.unit() != null ? line.unit() : "PIECE");
      le.setUnitPriceCentavos(line.unitPriceCentavos());
      le.setSubtotalCentavos(line.subtotalCentavos());
      le.setStockPolicy(
          line.stockPolicy() != null ? line.stockPolicy() : PosSaleStockPolicy.NOT_CONTROLLED);
      le.setSoldWithNegativeStock(line.soldWithNegativeStock());
      le.setSoldWhileUnavailable(line.soldWhileUnavailable());
      le.setRawBarcode(blankToNull(line.rawBarcode()));
      le.setCreatedAt(now);
      le.setUpdatedAt(now);
      le.setVersion(null);
      lineEntities.add(le);
    }
    e.setLines(lineEntities);

    List<PosSalePaymentJpaEntity> paymentEntities = new ArrayList<>();
    for (PosSale.Payment payment : domain.getPayments()) {
      PosSalePaymentJpaEntity pe = new PosSalePaymentJpaEntity();
      pe.setSale(e);
      pe.setPaymentId(payment.paymentId());
      pe.setMethod(payment.method() != null ? payment.method() : PosPaymentMethod.CASH);
      pe.setAmountCentavos(payment.amountCentavos());
      pe.setTenderedCentavos(payment.tenderedCentavos());
      pe.setChangeCentavos(payment.changeCentavos());
      pe.setCreatedAt(now);
      pe.setUpdatedAt(now);
      pe.setVersion(null);
      paymentEntities.add(pe);
    }
    e.setPayments(paymentEntities);
    return e;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
