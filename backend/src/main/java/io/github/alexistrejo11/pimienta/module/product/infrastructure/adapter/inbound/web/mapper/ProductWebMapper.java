package io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.product.core.application.command.CreateProductCommand;
import io.github.alexistrejo11.pimienta.module.product.core.application.command.UpdateProductCommand;
import io.github.alexistrejo11.pimienta.module.product.core.domain.Product;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request.ProductCreateRequest;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.request.ProductUpdateRequest;
import io.github.alexistrejo11.pimienta.module.product.infrastructure.adapter.inbound.web.dto.response.ProductResponse;

public final class ProductWebMapper {

  private ProductWebMapper() {}

  public static CreateProductCommand toCreateCommand(ProductCreateRequest request) {
    return new CreateProductCommand(
        request.name().strip(),
        request.description(),
        request.unit(),
        request.barcode(),
        request.trackStock());
  }

  public static UpdateProductCommand toUpdateCommand(ProductUpdateRequest request) {
    return new UpdateProductCommand(
        request.name().strip(),
        request.description(),
        request.unit(),
        request.barcode(),
        request.status(),
        request.trackStock());
  }

  public static ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getSku(),
        product.getName(),
        product.getDescription(),
        product.getUnit(),
        product.getBarcode(),
        product.getStatus(),
        product.isTrackStock(),
        product.getInventoryItemId(),
        product.getCreatedAt(),
        product.getUpdatedAt(),
        product.getVersion());
  }
}
