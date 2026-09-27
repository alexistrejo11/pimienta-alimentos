package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.supplier.core.application.command.UpsertSupplierCommand;
import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto.SupplierResponse;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto.SupplierSearchRequest;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto.UpsertSupplierRequest;
import java.util.List;

public final class SupplierWebMapper {

  private SupplierWebMapper() {}

  public static SupplierSearchCriteria toCriteria(
      SupplierSearchRequest filter, List<Long> scopeHeadquarterIds) {
    return new SupplierSearchCriteria(
        filter != null ? filter.getSearch() : null,
        filter != null ? filter.getHeadquarterId() : null,
        scopeHeadquarterIds);
  }

  public static UpsertSupplierCommand toCommand(UpsertSupplierRequest request) {
    List<Long> ids = request.headquarterIds() != null ? request.headquarterIds() : List.of();
    return new UpsertSupplierCommand(
        request.name(), request.contactName(), request.phone(), request.brand(), ids);
  }

  public static SupplierResponse toResponse(Supplier supplier) {
    return new SupplierResponse(
        supplier.getId(),
        supplier.getName(),
        supplier.getContactName(),
        supplier.getPhone(),
        supplier.getBrand(),
        supplier.getHeadquarterIds());
  }
}
