package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Identity of a warehouse item. Stock settings go through {@code /stock-settings}. */
public record ItemUpdateRequest(
    @NotBlank String name,
    String description,
    @NotNull ItemCategory category,
    @NotNull ItemUnit unit,
    String brand) {}
