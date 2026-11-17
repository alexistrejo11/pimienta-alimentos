package io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.doc;

import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.request.CreateContractRequest;
import io.github.alexistrejo11.pimienta.module.contract.adapter.inbound.web.dto.response.ContractResponse;
import io.github.alexistrejo11.pimienta.shared.web.openapi.doc.DocJwtSecured;
import io.swagger.v3.oas.annotations.Operation;
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

/** {@code POST /api/v1/contracts} */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@DocJwtSecured
@Operation(
    summary = "Create contract",
    description =
        """
        Registers a new contract. **EMPLOYEE** category requires `employeeId`. For **FIXED_TERM**, \
        `effectiveEnd` is required. **INDEFINITE** must not set `effectiveEnd`. May activate \
        related employment when category is EMPLOYEE. Rate limit: **STANDARD** (class default).""")
@RequestBody(
    required = true,
    description = "Cuerpo JSON del contrato a registrar (véase esquema **CreateContractRequest**).",
    content = {
      @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = CreateContractRequest.class),
          examples = {
            @ExampleObject(
                name = "supplierFixedTerm",
                summary = "Proveedor, plazo fijo",
                value =
                    """
                    {
                      "name": "Contrato suministro materias primas 2025",
                      "description": "Acuerdo marco con proveedor homologado.",
                      "category": "SUPPLIER",
                      "employeeId": null,
                      "termKind": "FIXED_TERM",
                      "effectiveStart": "2025-05-01",
                      "effectiveEnd": "2026-04-30",
                      "documentUrl": "https://storage.example.com/contracts/sup-2025-001.pdf",
                      "termsAndConditions": "Pago a 30 días; penalizaciones según cláusula 8.",
                      "referenceCode": "CT-SUP-2025-042",
                      "renewalCycleMonths": 12,
                      "agreedValue": 250000.00,
                      "currencyCode": "MXN"
                    }
                    """),
            @ExampleObject(
                name = "employeeIndefinite",
                summary = "Laboral, tiempo indefinido",
                value =
                    """
                    {
                      "name": "Contrato individual de trabajo — Ana Ruiz",
                      "description": "Operadora de línea, planta norte.",
                      "category": "EMPLOYEE",
                      "employeeId": 101,
                      "termKind": "INDEFINITE",
                      "effectiveStart": "2025-06-01",
                      "documentUrl": "https://storage.example.com/contracts/emp-101.pdf",
                      "termsAndConditions": null,
                      "referenceCode": "LAB-2025-101",
                      "renewalCycleMonths": null,
                      "agreedValue": null,
                      "currencyCode": null
                    }
                    """)
          })
    })
@ApiResponse(
    responseCode = "201",
    description = "Contract created.",
    content =
        @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ContractResponse.class)))
public @interface DocContractsCreate {}
