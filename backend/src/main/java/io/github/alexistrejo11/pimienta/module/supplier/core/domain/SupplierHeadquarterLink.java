package io.github.alexistrejo11.pimienta.module.supplier.core.domain;

/** One headquarter this person supplies, and whether that supply is current. */
public record SupplierHeadquarterLink(long headquarterId, boolean active) {}
