package io.github.alexistrejo11.pimienta.module.supplier.core.application.command;

import java.util.List;

public record UpsertSupplierCommand(
    String name, String contactName, String phone, String brand, List<Long> headquarterIds) {}
