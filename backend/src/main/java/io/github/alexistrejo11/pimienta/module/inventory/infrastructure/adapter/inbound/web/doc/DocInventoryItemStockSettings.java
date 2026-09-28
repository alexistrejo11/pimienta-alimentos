package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ItemStockSettingsRequest;
import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.response.ItemResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code PUT /api/v1/inventory/items/{id}/stock-settings} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameter(
    name = "id",
    in = ParameterIn.PATH,
    required = true,
    description = "Identificador del artículo.",
    example = "1")
@Operation(
    summary = "Update inventory item stock settings",
    description =
        "Actualiza costo, punto y cantidad de reorden y estado. Aplica a artículos de bodega y a"
            + " artículos de productos. **404** si no existe. Rate limit: **SENSITIVE_OPERATIONS**.")
@RequestBody(
    required = true,
    description = "Cuerpo JSON (**ItemStockSettingsRequest**).",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ItemStockSettingsRequest.class)))
@ApiResponse(
    responseCode = "200",
    description = "Artículo actualizado.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ItemResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "No encontrado.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocInventoryItemStockSettings {}
