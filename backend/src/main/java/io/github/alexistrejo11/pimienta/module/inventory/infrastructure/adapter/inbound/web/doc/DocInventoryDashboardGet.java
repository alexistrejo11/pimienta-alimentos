package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.InventoryDashboardResponse;
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

/** {@code GET /api/v1/inventory/dashboard} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameters({
  @Parameter(
      name = "headquarterId",
      in = ParameterIn.QUERY,
      description = "Filtrar por sede (MANAGER: sede asignada; ADMIN: opcional = todas).",
      example = "1")
})
@Operation(
    summary = "Inventory dashboard KPIs",
    description =
        """
        Conteos de SKU, bajo stock, sin existencias, conteos físicos abiertos y valor \
        disponible. Misma regla de estado que el resumen global. Rate limit: **READ_HEAVY**.""")
@ApiResponse(
    responseCode = "200",
    description = "KPIs de inventario.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = InventoryDashboardResponse.class)))
public @interface DocInventoryDashboardGet {}
