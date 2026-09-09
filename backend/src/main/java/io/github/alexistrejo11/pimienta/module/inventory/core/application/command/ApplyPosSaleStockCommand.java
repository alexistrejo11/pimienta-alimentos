package io.github.alexistrejo11.pimienta.module.inventory.core.application.command;

/**
 * Apply a POS sale (or inverse) stock change on the canonical POS location of a headquarter.
 *
 * @param quantityOut positive = decrease stock (sale); negative = increase (e.g. cancel/restock)
 * @param eventId optional reference for ledger notes (full idempotency in B4)
 * @param unitCost optional unit cost snapshot; defaults to item cost when null
 */
public record ApplyPosSaleStockCommand(
    long headquarterId,
    long itemId,
    int quantityOut,
    String eventId,
    java.math.BigDecimal unitCost,
    Long performedById) {}
