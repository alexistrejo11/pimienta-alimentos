package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PosSaleCategoryRequest(@NotBlank @Size(max = 64) String name, @Min(0) Integer displayOrder) {}
