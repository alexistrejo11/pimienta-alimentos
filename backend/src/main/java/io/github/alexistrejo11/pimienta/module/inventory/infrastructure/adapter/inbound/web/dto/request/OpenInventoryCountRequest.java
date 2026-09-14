package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull; import java.util.List;
public record OpenInventoryCountRequest(@NotNull Long locationId,@NotBlank String type,List<Long> itemIds) {}
