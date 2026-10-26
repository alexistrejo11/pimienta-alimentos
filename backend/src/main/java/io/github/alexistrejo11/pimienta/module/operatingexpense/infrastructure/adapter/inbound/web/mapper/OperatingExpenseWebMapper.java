package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command.CreateOperatingExpenseCommand;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command.UpdateOperatingExpenseCommand;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseCategoryTotal;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseListQuery;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSummary;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptStorage;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseAttachmentResponse;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseCategoryTotalResponse;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseDayTotalResponse;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseResponse;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseSearchRequest;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseSummaryResponse;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.UpsertOperatingExpenseRequest;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public final class OperatingExpenseWebMapper {

  private OperatingExpenseWebMapper() {}

  public static OperatingExpenseListQuery toQuery(OperatingExpenseSearchRequest request) {
    if (request == null) {
      return new OperatingExpenseListQuery(null, null, null, null, null);
    }
    return new OperatingExpenseListQuery(
        request.getSearch(),
        request.getHeadquarterId(),
        request.getFrom(),
        request.getTo(),
        request.getCategory());
  }

  public static CreateOperatingExpenseCommand toCreate(
      UpsertOperatingExpenseRequest request, List<MultipartFile> attachments) {
    return new CreateOperatingExpenseCommand(
        request.headquarterId(),
        request.amount(),
        request.category(),
        request.concept(),
        request.notes(),
        request.occurredOn(),
        attachments);
  }

  public static UpdateOperatingExpenseCommand toUpdate(
      UpsertOperatingExpenseRequest request, List<MultipartFile> attachments) {
    return new UpdateOperatingExpenseCommand(
        request.headquarterId(),
        request.amount(),
        request.category(),
        request.concept(),
        request.notes(),
        request.occurredOn(),
        request.retainAttachmentKeys(),
        attachments);
  }

  public static OperatingExpenseSummaryResponse toSummary(OperatingExpenseSummary summary) {
    OperatingExpenseCategoryTotal top = summary.topCategory();
    List<OperatingExpenseDayTotalResponse> byDay =
        summary.byDay().stream()
            .map(day -> new OperatingExpenseDayTotalResponse(day.occurredOn(), day.totalAmount(), day.count()))
            .toList();
    OperatingExpenseCategoryTotalResponse topResponse =
        top == null ? null : new OperatingExpenseCategoryTotalResponse(top.category(), top.totalAmount(), top.count());
    return new OperatingExpenseSummaryResponse(summary.totalAmount(), topResponse, byDay);
  }

  public static OperatingExpenseResponse toResponse(OperatingExpense expense, OperatingExpenseReceiptStorage storage) {
    List<OperatingExpenseAttachmentResponse> attachments =
        expense.getAttachmentKeys().stream()
            .map(key -> new OperatingExpenseAttachmentResponse(key, storage.presign(key)))
            .toList();
    return new OperatingExpenseResponse(
        expense.getId(),
        expense.getHeadquarterId(),
        expense.getAmount(),
        expense.getCategory(),
        expense.getConcept(),
        expense.getNotes(),
        expense.getOccurredOn(),
        expense.getRecordedByUserId(),
        attachments,
        expense.getCreatedAt(),
        expense.getUpdatedAt(),
        expense.getVersion());
  }
}
