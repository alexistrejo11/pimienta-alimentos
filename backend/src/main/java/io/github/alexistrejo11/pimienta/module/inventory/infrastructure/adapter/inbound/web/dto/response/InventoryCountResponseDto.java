package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response;
public record InventoryCountResponseDto(Long itemId,Integer expectedQuantity,Integer countedQuantity,Integer variance) {}
