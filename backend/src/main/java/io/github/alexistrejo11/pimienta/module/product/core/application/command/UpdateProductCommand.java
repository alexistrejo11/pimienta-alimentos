package io.github.alexistrejo11.pimienta.module.product.core.application.command;

import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Status;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product.Unit;

public record UpdateProductCommand(
    String name,
    String description,
    Unit unit,
    String barcode,
    Status status,
    boolean trackStock) {}
