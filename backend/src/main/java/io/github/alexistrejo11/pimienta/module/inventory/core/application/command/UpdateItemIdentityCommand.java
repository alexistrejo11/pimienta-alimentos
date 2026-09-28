package io.github.alexistrejo11.pimienta.module.inventory.core.application.command;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemCategory;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item.ItemUnit;

/** What a warehouse item is. Only items without a product accept it. */
public record UpdateItemIdentityCommand(
    String name, String description, ItemCategory category, ItemUnit unit, String brand) {}
