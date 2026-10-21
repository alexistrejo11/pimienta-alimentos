package io.github.alexistrejo11.pimienta.module.inventory.core.application.command;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason;
import java.math.BigDecimal;
import java.util.List;

public final class InventoryTransactionCommands {

  private InventoryTransactionCommands() {}

  public record InTransactionCommand(
      String externalReference, String notes, Long initiatedById, List<InLine> lines) {}

  public record InLine(long itemId, long locationId, int quantity, BigDecimal unitCost) {}

  public record OutTransactionCommand(
      String externalReference,
      String notes,
      InventoryExitReason exitReason,
      Long initiatedById,
      List<OutLine> lines) {}

  public record OutLine(long itemId, long locationId, int quantity, BigDecimal unitCost) {}

  public record AdjustmentTransactionCommand(
      String externalReference, String notes, Long initiatedById, List<AdjustmentLine> lines) {}

  public record AdjustmentLine(long itemId, long locationId, int countedQuantity, String reason) {}
}
