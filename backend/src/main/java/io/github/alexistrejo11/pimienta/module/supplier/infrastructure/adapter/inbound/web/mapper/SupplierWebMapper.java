package io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.supplier.core.application.command.UpsertSupplierCommand;
import io.github.alexistrejo11.pimienta.module.supplier.core.application.query.SupplierSearchCriteria;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.Supplier;
import io.github.alexistrejo11.pimienta.module.supplier.core.domain.SupplierHeadquarterLink;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto.SupplierHeadquarterRequest;
import io.github.alexistrejo11.pimienta.module.supplier.infrastructure.adapter.inbound.web.dto.SupplierHeadquarterResponse;
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
    List<SupplierHeadquarterRequest> rows =
        request.headquarters() != null ? request.headquarters() : List.of();
    List<SupplierHeadquarterLink> links =
        rows.stream()
            .map(row -> new SupplierHeadquarterLink(row.headquarterId(), Boolean.TRUE.equals(row.active())))
            .toList();
    return new UpsertSupplierCommand(request.name(), request.phone(), request.brand(), links);
  }

  public static SupplierResponse toResponse(Supplier supplier) {
    List<SupplierHeadquarterResponse> headquarters =
        supplier.getHeadquarters().stream()
            .map(link -> new SupplierHeadquarterResponse(link.headquarterId(), link.active()))
            .toList();
    return new SupplierResponse(
        supplier.getId(), supplier.getName(), supplier.getPhone(), supplier.getBrand(), headquarters);
  }
}
