package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web;

import static io.github.alexistrejo11.pimienta.shared.web.ApiPaths.BASE;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.input.OperatingExpenseManagementUseCases;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output.OperatingExpenseReceiptStorage;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc.DocOperatingExpenseCreate;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc.DocOperatingExpenseDelete;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc.DocOperatingExpenseGet;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc.DocOperatingExpenseList;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc.DocOperatingExpenseSummary;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc.DocOperatingExpenseUpdate;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.doc.DocOperatingExpenses;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseResponse;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseSearchRequest;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.OperatingExpenseSummaryResponse;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto.UpsertOperatingExpenseRequest;
import io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.mapper.OperatingExpenseWebMapper;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimit;
import io.github.alexistrejo11.pimienta.shared.ratelimit.RateLimitProfile;
import io.github.alexistrejo11.pimienta.shared.web.PagedResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(BASE + "/operating-expenses")
@RateLimit(profile = RateLimitProfile.STANDARD)
@DocOperatingExpenses
public class OperatingExpenseController {

  private final OperatingExpenseManagementUseCases expenseUseCases;
  private final OperatingExpenseReceiptStorage receiptStorage;

  public OperatingExpenseController(
      OperatingExpenseManagementUseCases expenseUseCases, OperatingExpenseReceiptStorage receiptStorage) {
    this.expenseUseCases = expenseUseCases;
    this.receiptStorage = receiptStorage;
  }

  @GetMapping
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOperatingExpenseList
  public PagedResponse<OperatingExpenseResponse> list(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @ModelAttribute OperatingExpenseSearchRequest filter) {
    OperatingExpenseSearchRequest query = filter != null ? filter : new OperatingExpenseSearchRequest();
    return PagedResponse.map(
        expenseUseCases.search(principal, OperatingExpenseWebMapper.toQuery(query), query.toPageable()),
        expense -> OperatingExpenseWebMapper.toResponse(expense, receiptStorage));
  }

  @GetMapping("/summary")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOperatingExpenseSummary
  public OperatingExpenseSummaryResponse summary(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @ModelAttribute OperatingExpenseSearchRequest filter) {
    OperatingExpenseSearchRequest query = filter != null ? filter : new OperatingExpenseSearchRequest();
    return OperatingExpenseWebMapper.toSummary(
        expenseUseCases.summarize(principal, OperatingExpenseWebMapper.toQuery(query)));
  }

  @GetMapping("/{id}")
  @RateLimit(profile = RateLimitProfile.READ_HEAVY)
  @DocOperatingExpenseGet
  public OperatingExpenseResponse get(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable long id) {
    return OperatingExpenseWebMapper.toResponse(expenseUseCases.getById(principal, id), receiptStorage);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOperatingExpenseCreate
  public OperatingExpenseResponse create(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @Valid @RequestPart("data") UpsertOperatingExpenseRequest data,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
    OperatingExpense created = expenseUseCases.create(principal, OperatingExpenseWebMapper.toCreate(data, attachments));
    return OperatingExpenseWebMapper.toResponse(created, receiptStorage);
  }

  @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOperatingExpenseUpdate
  public OperatingExpenseResponse update(
      @AuthenticationPrincipal JwtAuthenticationContext principal,
      @PathVariable long id,
      @Valid @RequestPart("data") UpsertOperatingExpenseRequest data,
      @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
    OperatingExpense updated =
        expenseUseCases.update(principal, id, OperatingExpenseWebMapper.toUpdate(data, attachments));
    return OperatingExpenseWebMapper.toResponse(updated, receiptStorage);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RateLimit(profile = RateLimitProfile.SENSITIVE_OPERATIONS)
  @DocOperatingExpenseDelete
  public void delete(
      @AuthenticationPrincipal JwtAuthenticationContext principal, @PathVariable long id) {
    expenseUseCases.delete(principal, id);
  }
}
