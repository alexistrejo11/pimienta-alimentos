package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request.UpdateContractRequest;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.response.ContractResponse;
import io.github.alexistrejo11.pimienta.shared.exception.api.ApiErrorResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@code PUT /api/v1/contracts/{id}} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Parameter(
    name = "id",
    in = ParameterIn.PATH,
    required = true,
    description = "Identificador del contrato a reemplazar.",
    example = "42")
@Operation(
    summary = "Replace contract",
    description =
        """
        Full update of contract fields (same rules as create for term dates and relationships). \
        **404** if the contract does not exist.""")
@RequestBody(
    required = true,
    description = "Estado completo del contrato (**UpdateContractRequest**).",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = UpdateContractRequest.class),
            examples = {
              @ExampleObject(
                  name = "updateSupplier",
                  summary = "Actualización proveedor",
                  value =
                      """
                      {
                        "name": "Contrato suministro 2025 (enmienda 1)",
                        "description": "Incluye anexo de logística inversa.",
                        "category": "SUPPLIER",
                        "employeeId": null,
                        "termKind": "FIXED_TERM",
                        "effectiveStart": "2025-05-01",
                        "effectiveEnd": "2026-10-31",
                        "documentUrl": "https://storage.example.com/contracts/sup-2025-001-v2.pdf",
                        "termsAndConditions": "Pago a 45 días.",
                        "referenceCode": "CT-SUP-2025-042-R1",
                        "renewalCycleMonths": 12,
                        "agreedValue": 275000.00,
                        "currencyCode": "MXN"
                      }
                      """)
            }))
@ApiResponse(
    responseCode = "200",
    description = "Updated contract.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ContractResponse.class)))
@ApiResponse(
    responseCode = "404",
    description = "Contract not found (`CONTRACT_NOT_FOUND`).",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)))
public @interface DocContractsUpdate {}
