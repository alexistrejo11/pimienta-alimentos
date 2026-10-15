package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OperatingExpenseAttachmentResponse")
public record OperatingExpenseAttachmentResponse(
    @Schema(description = "Private S3 object key.") String key,
    @Schema(description = "Presigned GET URL valid for one hour.") String url) {}
