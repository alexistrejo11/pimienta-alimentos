package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public record CreateOperatingExpenseCommand(
    Long headquarterId,
    BigDecimal amount,
    OperatingExpenseCategory category,
    String concept,
    String notes,
    LocalDate occurredOn,
    List<MultipartFile> attachments) {}
