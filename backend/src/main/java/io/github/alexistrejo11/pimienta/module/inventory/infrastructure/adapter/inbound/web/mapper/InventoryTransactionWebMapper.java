package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.AdjustmentTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.InTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.OutTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryMovementResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryTransactionResponse;
import java.util.List;

public final class InventoryTransactionWebMapper {

  private InventoryTransactionWebMapper() {}

  public static InventoryTransactionResponse toResponse(InventoryTransaction tx) {
    List<InventoryMovementResponse> movements =
        tx.getMovements().stream().map(InventoryMovementWebMapper::toResponse).toList();
    return new InventoryTransactionResponse(
        tx.getId(),
        tx.getTransactionNumber(),
        tx.getType(),
        tx.getStatus(),
        tx.getExternalReference(),
        tx.getNotes(),
        tx.getExitReason(),
        tx.getInitiatedById(),
        tx.getApprovedById(),
        tx.getApprovedAt(),
        tx.getCompletedAt(),
        tx.getCreatedAt(),
        tx.getUpdatedAt(),
        tx.getDeletedAt(),
        tx.getVersion(),
        movements);
  }

  public static InventoryTransactionCommands.InTransactionCommand toCommand(InTransactionRequest request) {
    List<InventoryTransactionCommands.InLine> lines =
        request.lines().stream()
            .map(l -> new InventoryTransactionCommands.InLine(l.itemId(), l.locationId(), l.quantity(), l.unitCost()))
            .toList();
    return new InventoryTransactionCommands.InTransactionCommand(
        request.externalReference(), request.notes(), request.initiatedById(), lines);
  }

  public static InventoryTransactionCommands.OutTransactionCommand toCommand(OutTransactionRequest request) {
    List<InventoryTransactionCommands.OutLine> lines =
        request.lines().stream()
            .map(l -> new InventoryTransactionCommands.OutLine(l.itemId(), l.locationId(), l.quantity(), l.unitCost()))
            .toList();
    return new InventoryTransactionCommands.OutTransactionCommand(
        request.externalReference(),
        request.notes(),
        request.exitReason(),
        request.initiatedById(),
        lines);
  }

  public static InventoryTransactionCommands.AdjustmentTransactionCommand toCommand(
      AdjustmentTransactionRequest request) {
    List<InventoryTransactionCommands.AdjustmentLine> lines =
        request.lines().stream()
            .map(
                l ->
                    new InventoryTransactionCommands.AdjustmentLine(
                        l.itemId(), l.locationId(), l.countedQuantity(), l.reason()))
            .toList();
    return new InventoryTransactionCommands.AdjustmentTransactionCommand(
        request.externalReference(), request.notes(), request.initiatedById(), lines);
  }
}
