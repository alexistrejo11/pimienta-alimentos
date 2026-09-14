package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.ApplyPosSaleStockCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosSaleInventoryUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStockPolicy;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSaleRepository;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Applies POS ledger events that mutate central POS stock (sale cancel reversal only). */
@Service
public class PosEventStockProjector {

  private static final Logger log = LoggerFactory.getLogger(PosEventStockProjector.class);

  private final PosSaleInventoryUseCases posSaleInventoryUseCases;
  private final PosSaleRepository saleRepository;

  public PosEventStockProjector(
      PosSaleInventoryUseCases posSaleInventoryUseCases, PosSaleRepository saleRepository) {
    this.posSaleInventoryUseCases = posSaleInventoryUseCases;
    this.saleRepository = saleRepository;
  }

  public void project(
      long headquarterId, UUID eventId, String eventType, UUID aggregateId, String payloadJson) {
    if ("SALE_CANCELLED".equals(eventType)) {
      reverseCancelledSale(headquarterId, eventId, aggregateId);
    }
  }

  private void reverseCancelledSale(long headquarterId, UUID eventId, UUID saleId) {
    if (saleId == null) {
      return;
    }
    Optional<PosSale> sale = saleRepository.findBySaleId(saleId);
    if (sale.isEmpty()) {
      return;
    }
    PosSale existing = sale.get();
    if (existing.getHeadquarterId() != headquarterId) {
      return;
    }
    for (PosSale.Line line : existing.getLines()) {
      if (line.productId() == null || line.quantity() == 0) {
        continue;
      }
      if (line.stockPolicy() != PosSaleStockPolicy.CONTROLLED) {
        continue;
      }
      posSaleInventoryUseCases.applySaleStock(
          new ApplyPosSaleStockCommand(
              headquarterId,
              line.productId(),
              -line.quantity(),
              eventId.toString(),
              null,
              null));
    }
    log.info(
        "POS stock projected eventType=SALE_CANCELLED headquarterId={} saleId={}",
        headquarterId,
        saleId);
  }
}
