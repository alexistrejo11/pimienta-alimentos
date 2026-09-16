package io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertHeadquarterItemCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpsertPosSettingsCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.PosOperationalConfig;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogItemRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.HeadquarterPosCatalogItemResponse;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.PosSettingsRequest;
import io.github.alexistrejo11.pimienta.module.headquarter.infrastructure.adapter.inbound.web.dto.PosSettingsResponse;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;

public final class HeadquarterPosWebMapper {

  private HeadquarterPosWebMapper() {}

  public static UpsertPosSettingsCommand toCommand(PosSettingsRequest request) {
    return new UpsertPosSettingsCommand(
        request.currency(),
        request.catalogStaleWarnHours(),
        request.catalogStaleBlockHours(),
        request.openAmountCategories(),
        request.allowOpenProducts(),
        request.defaultNegativeStockLimit());
  }

  public static PosSettingsResponse toResponse(PosOperationalConfig config) {
    return new PosSettingsResponse(
        config.getId(),
        config.getHeadquarterId(),
        config.getCurrency(),
        config.getCatalogStaleWarnHours(),
        config.getCatalogStaleBlockHours(),
        config.getOpenAmountCategories(),
        config.isAllowOpenProducts(),
        config.getDefaultNegativeStockLimit(),
        config.getCreatedAt(),
        config.getUpdatedAt(),
        config.getVersion());
  }

  public static UpsertHeadquarterItemCommand toCommand(HeadquarterPosCatalogItemRequest request) {
    return new UpsertHeadquarterItemCommand(
        request.saleCategory(),
        request.salePrice(),
        request.available(),
        request.stockPolicy(),
        request.negativeStockLimit());
  }

  public static HeadquarterPosCatalogItemResponse toResponse(HeadquarterItem item) {
    return new HeadquarterPosCatalogItemResponse(
        item.getId(),
        item.getHeadquarterId(),
        item.getItemId(),
        "",
        "",
        null,
        item.getSaleCategory(),
        item.getSalePrice(),
        item.isAvailable(),
        item.getStockPolicy(),
        item.getNegativeStockLimit(),
        item.getCreatedAt(),
        item.getUpdatedAt(),
        item.getVersion());
  }

  public static HeadquarterPosCatalogItemResponse toResponse(HeadquarterItem row, Item item) {
    return new HeadquarterPosCatalogItemResponse(row.getId(), row.getHeadquarterId(), row.getItemId(),
        item.getName(), item.getSku(), item.getBarcode(), row.getSaleCategory(), row.getSalePrice(),
        row.isAvailable(), row.getStockPolicy(), row.getNegativeStockLimit(), row.getCreatedAt(),
        row.getUpdatedAt(), row.getVersion());
  }
}
