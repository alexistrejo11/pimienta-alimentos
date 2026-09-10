package io.github.alexistrejo11.pimienta.module.pos.core.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
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

/** Applies POS ledger events (waste, restock, cancel) to central POS stock. */
@Service
public class PosEventStockProjector {

  private static final Logger log = LoggerFactory.getLogger(PosEventStockProjector.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final PosSaleInventoryUseCases posSaleInventoryUseCases;
  private final PosSaleRepository saleRepository;
  private final HeadquarterItemRepository headquarterItemRepository;

  public PosEventStockProjector(
      PosSaleInventoryUseCases posSaleInventoryUseCases,
      PosSaleRepository saleRepository,
      HeadquarterItemRepository headquarterItemRepository) {
    this.posSaleInventoryUseCases = posSaleInventoryUseCases;
    this.saleRepository = saleRepository;
    this.headquarterItemRepository = headquarterItemRepository;
  }

  public void project(
      long headquarterId, UUID eventId, String eventType, UUID aggregateId, String payloadJson) {
    switch (eventType) {
      case "WASTE_RECORDED" -> applyQuantityDelta(headquarterId, eventId, payloadJson, true);
      case "RESTOCK_RECORDED" -> applyQuantityDelta(headquarterId, eventId, payloadJson, false);
      case "SALE_CANCELLED" -> reverseCancelledSale(headquarterId, eventId, aggregateId);
      default -> {}
    }
  }

  private void applyQuantityDelta(
      long headquarterId, UUID eventId, String payloadJson, boolean waste) {
    JsonNode payload = parse(payloadJson);
    if (payload == null) {
      return;
    }
    Long productId = readLong(payload, "productId");
    int quantity = readInt(payload, "quantity");
    if (productId == null || quantity <= 0) {
      return;
    }
    if (!isControlled(headquarterId, productId)) {
      return;
    }
    int quantityOut = waste ? quantity : -quantity;
    posSaleInventoryUseCases.applySaleStock(
        new ApplyPosSaleStockCommand(
            headquarterId, productId, quantityOut, eventId.toString(), null, null));
    log.info(
        "POS stock projected eventType={} headquarterId={} productId={} qty={}",
        waste ? "WASTE" : "RESTOCK",
        headquarterId,
        productId,
        quantity);
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
  }

  private boolean isControlled(long headquarterId, long productId) {
    return headquarterItemRepository
        .findByHeadquarterIdAndItemId(headquarterId, productId)
        .map(item -> item.getStockPolicy() == HeadquarterItem.StockPolicy.CONTROLLED)
        .orElse(false);
  }

  private JsonNode parse(String payloadJson) {
    if (payloadJson == null || payloadJson.isBlank()) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readTree(payloadJson);
    } catch (Exception ex) {
      log.warn("Could not parse POS event payload for stock projection: {}", ex.getMessage());
      return null;
    }
  }

  private static Long readLong(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return null;
    }
    if (value.isNumber()) {
      return value.longValue();
    }
    if (value.isTextual() && !value.asText().isBlank()) {
      try {
        return Long.parseLong(value.asText().trim());
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private static int readInt(JsonNode node, String field) {
    JsonNode value = node.get(field);
    if (value == null || value.isNull()) {
      return 0;
    }
    return value.asInt(0);
  }
}
