package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Item;
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

/** {@code GET /api/v1/inventory/items} */
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
  @Parameter(name = "name", in = ParameterIn.QUERY, description = "Filtro por nombre (coincidencia parcial)."),
  @Parameter(name = "sku", in = ParameterIn.QUERY, description = "Filtro por SKU."),
  @Parameter(
      name = "category",
      in = ParameterIn.QUERY,
      description = "Filtro por categoría.",
      schema = @Schema(implementation = Item.ItemCategory.class)),
  @Parameter(
      name = "status",
      in = ParameterIn.QUERY,
      description = "Filtro por estado del artículo.",
      schema = @Schema(implementation = Item.ItemStatus.class)),
  @Parameter(
      name = "kind",
      in = ParameterIn.QUERY,
      description =
          "`PRODUCT`: artículos que respaldan un producto con stock. `WAREHOUSE`: artículos de"
              + " bodega sin producto.",
      schema = @Schema(implementation = Item.ItemKind.class))
})
@Operation(
    summary = "Search inventory items",
    description = "Lista paginada con filtros opcionales. Respuesta: **PagedResponse**. Rate limit: **READ_HEAVY**.")
@ApiResponse(
    responseCode = "200",
    description = "Página de artículos.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PagedResponse.class)))
public @interface DocInventoryItemSearch {}
