package io.github.alexistrejo11.pimienta.module.operatingexpense.infrastructure.adapter.inbound.web.dto;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.enums.OperatingExpenseCategory;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class OperatingExpenseSearchRequest extends PageableRequest {

  private String search;
  private Long headquarterId;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate from;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate to;

  private OperatingExpenseCategory category;

  public String getSearch() {
    return search;
  }

  public void setSearch(String search) {
    this.search = search;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public void setHeadquarterId(Long headquarterId) {
    this.headquarterId = headquarterId;
  }

  public LocalDate getFrom() {
    return from;
  }

  public void setFrom(LocalDate from) {
    this.from = from;
  }

  public LocalDate getTo() {
    return to;
  }

  public void setTo(LocalDate to) {
    this.to = to;
  }

  public OperatingExpenseCategory getCategory() {
    return category;
  }

  public void setCategory(OperatingExpenseCategory category) {
    this.category = category;
  }
}
