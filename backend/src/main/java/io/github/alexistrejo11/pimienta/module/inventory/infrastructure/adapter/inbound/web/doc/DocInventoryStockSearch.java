package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code GET /api/v1/inventory/stock} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameters({
  @Parameter(
      name = "page",
      in = ParameterIn.QUERY,
      description = "Índice de página (base 0).",
      example = "0",
      schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
  @Parameter(
      name = "size",
      in = ParameterIn.QUERY,
      description = "Tamaño de página (1–100).",
      example = "20",
      schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")),
  @Parameter(name = "itemId", in = ParameterIn.QUERY, description = "Filtrar por artículo.", example = "10"),
  @Parameter(name = "locationId", in = ParameterIn.QUERY, description = "Filtrar por ubicación.", example = "3"),
  @Parameter(
      name = "status",
      in = ParameterIn.QUERY,
      description = "Estado del registro de inventario.",
      schema = @Schema(implementation = Inventory.InventoryStatus.class)),
  @Parameter(
      name = "headquarterId",
      in = ParameterIn.QUERY,
      description = "Filtrar por sede (MANAGER: sede asignada; ADMIN: opcional).",
      example = "1")
})
@Operation(
    summary = "Search stock rows",
    description = "Búsqueda paginada de filas de existencia. Rate limit: **READ_HEAVY**.")
@ApiResponse(
    responseCode = "200",
    description = "Página de existencias.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PagedResponse.class)))
public @interface DocInventoryStockSearch {}
