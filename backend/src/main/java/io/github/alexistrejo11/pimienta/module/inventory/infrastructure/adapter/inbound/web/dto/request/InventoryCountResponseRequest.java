package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;
import jakarta.validation.constraints.Min; import jakarta.validation.constraints.NotNull;
public record InventoryCountResponseRequest(@NotNull Long itemId,@NotNull @Min(0) Integer countedQuantity) {}
