package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryMovement;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.enums.InventoryExitReason;
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

/** {@code GET /api/v1/inventory/movements} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameters({
  @Parameter(
      name = "page",
      in = ParameterIn.QUERY,
      schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
  @Parameter(
      name = "size",
      in = ParameterIn.QUERY,
      schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "100")),
  @Parameter(
      name = "type",
      in = ParameterIn.QUERY,
      schema = @Schema(implementation = InventoryMovement.InventoryMovementType.class)),
  @Parameter(
      name = "direction",
      in = ParameterIn.QUERY,
      schema = @Schema(implementation = InventoryMovement.MovementDirection.class)),
  @Parameter(
      name = "exitReason",
      in = ParameterIn.QUERY,
      schema = @Schema(implementation = InventoryExitReason.class)),
  @Parameter(name = "itemId", in = ParameterIn.QUERY),
  @Parameter(name = "locationId", in = ParameterIn.QUERY),
  @Parameter(name = "fromDate", in = ParameterIn.QUERY, description = "ISO-8601 date-time"),
  @Parameter(name = "toDate", in = ParameterIn.QUERY, description = "ISO-8601 date-time")
})
@Operation(summary = "Search inventory movements", description = "Lista paginada. Rate limit: **READ_HEAVY**.")
@ApiResponse(
    responseCode = "200",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PagedResponse.class)))
public @interface DocInventoryMovementSearch {}
