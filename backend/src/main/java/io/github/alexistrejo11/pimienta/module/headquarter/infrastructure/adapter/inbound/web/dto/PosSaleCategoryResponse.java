package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto;

public record PosSaleCategoryResponse(long id, long headquarterId, String name, int displayOrder, boolean active) {}
