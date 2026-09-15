package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.InventoryTransactionCommands;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.AdjustmentTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.PhysicalAdjustmentTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.PurchaseTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ReturnClientTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ReturnSupplierTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.SaleTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ScrapTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.TransferTransactionRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryMovementResponse;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryTransactionResponse;
import java.util.List;

public final class InventoryTransactionWebMapper {

    private InventoryTransactionWebMapper() {
    }

    public static InventoryTransactionResponse toResponse(InventoryTransaction tx) {
        List<InventoryMovementResponse> movements = tx.getMovements().stream()
                .map(InventoryMovementWebMapper::toResponse).toList();
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

    public static InventoryTransactionCommands.PurchaseTransactionCommand toCommand(
            PurchaseTransactionRequest request) {
        List<InventoryTransactionCommands.PurchaseLine> lines = request.lines().stream()
                .map(
                        l -> new InventoryTransactionCommands.PurchaseLine(
                                l.itemId(), l.locationId(), l.quantity(), l.unitCost()))
                .toList();
        return new InventoryTransactionCommands.PurchaseTransactionCommand(
                request.externalReference(), request.notes(), request.initiatedById(), lines);
    }

    public static InventoryTransactionCommands.SaleTransactionCommand toCommand(
            SaleTransactionRequest request) {
        List<InventoryTransactionCommands.SaleLine> lines = request.lines().stream()
                .map(
                        l -> new InventoryTransactionCommands.SaleLine(
                                l.itemId(), l.locationId(), l.quantity(), l.unitCost()))
                .toList();
        return new InventoryTransactionCommands.SaleTransactionCommand(
                request.externalReference(), request.notes(), request.initiatedById(), lines);
    }

    public static InventoryTransactionCommands.TransferTransactionCommand toCommand(
            TransferTransactionRequest request) {
        List<InventoryTransactionCommands.TransferLine> lines = request.lines().stream()
                .map(
                        l -> new InventoryTransactionCommands.TransferLine(
                                l.itemId(),
                                l.fromLocationId(),
                                l.toLocationId(),
                                l.quantity(),
                                l.unitCost()))
                .toList();
        return new InventoryTransactionCommands.TransferTransactionCommand(
                request.externalReference(), request.notes(), request.initiatedById(), lines);
    }

    public static InventoryTransactionCommands.AdjustmentTransactionCommand toCommand(
            AdjustmentTransactionRequest request) {
        List<InventoryTransactionCommands.AdjustmentLine> lines = request.lines().stream()
                .map(
                        l -> new InventoryTransactionCommands.AdjustmentLine(
                                l.itemId(), l.locationId(), l.newQuantity(), l.reason()))
                .toList();
        return new InventoryTransactionCommands.AdjustmentTransactionCommand(
                request.externalReference(), request.notes(), request.initiatedById(), lines);
    }

    public static InventoryTransactionCommands.ReturnClientCommand toCommand(
            ReturnClientTransactionRequest request) {
        List<InventoryTransactionCommands.ReturnClientLine> lines = request.lines().stream()
                .map(
                        l -> new InventoryTransactionCommands.ReturnClientLine(
                                l.itemId(), l.locationId(), l.quantity(), l.unitCost()))
                .toList();
        return new InventoryTransactionCommands.ReturnClientCommand(
                request.externalReference(), request.notes(), request.initiatedById(), lines);
    }

    public static InventoryTransactionCommands.ReturnSupplierCommand toCommand(
            ReturnSupplierTransactionRequest request) {
        List<InventoryTransactionCommands.ReturnSupplierLine> lines = request.lines().stream()
                .map(
                        l -> new InventoryTransactionCommands.ReturnSupplierLine(
                                l.itemId(), l.locationId(), l.quantity(), l.unitCost()))
                .toList();
        return new InventoryTransactionCommands.ReturnSupplierCommand(
                request.externalReference(), request.notes(), request.initiatedById(), lines);
    }

    public static InventoryTransactionCommands.ScrapCommand toCommand(ScrapTransactionRequest request) {
        List<InventoryTransactionCommands.ScrapLine> lines = request.lines().stream()
                .map(
                        l -> new InventoryTransactionCommands.ScrapLine(
                                l.itemId(), l.locationId(), l.quantity(), l.unitCost()))
                .toList();
        return new InventoryTransactionCommands.ScrapCommand(
                request.externalReference(),
                request.notes(),
                request.exitReason(),
                request.initiatedById(),
                lines);
    }

    public static InventoryTransactionCommands.PhysicalAdjustmentCommand toCommand(
            PhysicalAdjustmentTransactionRequest request) {
        return new InventoryTransactionCommands.PhysicalAdjustmentCommand(
                request.inventoryId(), request.newQuantity(), request.reason(), request.performedById());
    }
}
