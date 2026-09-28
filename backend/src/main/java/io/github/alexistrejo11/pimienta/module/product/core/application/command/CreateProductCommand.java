package io.github.alexistrejo11.pimienta.module.product.core.application.command;

import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;

public record CreateProductCommand(
    String name, String description, Unit unit, String barcode, boolean trackStock) {}
