package io.github.alexistrejo11.pimienta.module.supplier.core.application.command;

import io.github.alexistrejo11.pimienta.module.supplier.core.domain.SupplierHeadquarterLink;
import java.util.List;

public record UpsertSupplierCommand(
    String name, String phone, String brand, List<SupplierHeadquarterLink> headquarters) {}
