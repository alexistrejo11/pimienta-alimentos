package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request.ItemUpdateRequest;
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

/** {@code PUT /api/v1/inventory/items/{id}} */
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
    summary = "Update warehouse item identity",
    description =
        "Actualiza nombre, descripción, categoría, unidad y marca de un artículo de bodega."
            + " **409** `ITEM_LINKED_TO_PRODUCT` si pertenece a un producto (se edita en Productos)."
            + " **404** si no existe. Rate limit: **SENSITIVE_OPERATIONS**.")
@RequestBody(
    required = true,
    description = "Cuerpo JSON (**ItemUpdateRequest**).",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ItemUpdateRequest.class)))
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
@ApiResponse(
    responseCode = "409",
    description = "El artículo pertenece a un producto.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocInventoryItemUpdate {}
