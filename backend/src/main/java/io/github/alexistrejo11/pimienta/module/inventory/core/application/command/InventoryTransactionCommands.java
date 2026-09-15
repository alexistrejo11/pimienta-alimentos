package io.github.alexistrejo11.pimienta.module.inventory.core.application.command;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason;
import java.math.BigDecimal;
import java.util.List;

public final class InventoryTransactionCommands {

    private InventoryTransactionCommands() {
    }

    public record PurchaseTransactionCommand(
            String externalReference, String notes, Long initiatedById, List<PurchaseLine> lines) {
    }

    public record PurchaseLine(long itemId, long locationId, int quantity, BigDecimal unitCost) {
    }

    public record SaleTransactionCommand(
            String externalReference, String notes, Long initiatedById, List<SaleLine> lines) {
    }

    public record SaleLine(long itemId, long locationId, int quantity, BigDecimal unitCost) {
    }

    public record TransferTransactionCommand(
            String externalReference, String notes, Long initiatedById, List<TransferLine> lines) {
    }

    public record TransferLine(
            long itemId, long fromLocationId, long toLocationId, int quantity, BigDecimal unitCost) {
    }

    public record AdjustmentTransactionCommand(
            String externalReference, String notes, Long initiatedById, List<AdjustmentLine> lines) {
    }

    public record AdjustmentLine(long itemId, long locationId, int newQuantity, String reason) {
    }

    public record ReturnClientCommand(
            String externalReference, String notes, Long initiatedById, List<ReturnClientLine> lines) {
    }

    public record ReturnClientLine(long itemId, long locationId, int quantity, BigDecimal unitCost) {
    }

    public record ReturnSupplierCommand(
            String externalReference, String notes, Long initiatedById, List<ReturnSupplierLine> lines) {
    }

    public record ReturnSupplierLine(long itemId, long locationId, int quantity, BigDecimal unitCost) {
    }

    public record ScrapCommand(
            String externalReference,
            String notes,
            InventoryExitReason exitReason,
            Long initiatedById,
            List<ScrapLine> lines) {
    }

    public record ScrapLine(long itemId, long locationId, int quantity, BigDecimal unitCost) {
    }

    public record PhysicalAdjustmentCommand(
            long inventoryId, int newQuantity, String reason, Long performedById) {
    }
}
