package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(name = "OperatingExpenseResponse")
public record OperatingExpenseResponse(
    long id,
    long headquarterId,
    BigDecimal amount,
    OperatingExpenseCategory category,
    String concept,
    String notes,
    LocalDate occurredOn,
    long recordedByUserId,
    List<OperatingExpenseAttachmentResponse> attachments,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    Long version) {}
