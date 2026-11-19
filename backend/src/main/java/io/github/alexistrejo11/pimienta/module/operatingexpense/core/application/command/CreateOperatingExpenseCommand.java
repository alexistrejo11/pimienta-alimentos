package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import io.github.alexistrejo11.pimienta.shared.image.ImageUploadPayload;
import java.util.List;

public record CreateOperatingExpenseCommand(
    Long headquarterId,
    BigDecimal amount,
    OperatingExpenseCategory category,
    String concept,
    String notes,
    LocalDate occurredOn,
    List<ImageUploadPayload> attachments) {}
