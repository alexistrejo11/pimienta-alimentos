package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.inbound.web.dto.request;

import io.github.alexistrejo11.pimienta.module.inventory.core.application.query.InventoryTransactionSearchCriteria;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionStatus;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.InventoryTransaction.TransactionType;
import io.github.alexistrejo11.pimienta.shared.web.PageableRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class InventoryTransactionSearchRequest extends PageableRequest {

  private TransactionType type;
  private TransactionStatus status;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime fromDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime toDate;

  private Long initiatedById;

  public TransactionType getType() {
    return type;
  }

  public void setType(TransactionType type) {
    this.type = type;
  }

  public TransactionStatus getStatus() {
    return status;
  }

  public void setStatus(TransactionStatus status) {
    this.status = status;
  }

  public LocalDateTime getFromDate() {
    return fromDate;
  }

  public void setFromDate(LocalDateTime fromDate) {
    this.fromDate = fromDate;
  }

  public LocalDateTime getToDate() {
    return toDate;
  }

  public void setToDate(LocalDateTime toDate) {
    this.toDate = toDate;
  }

  public Long getInitiatedById() {
    return initiatedById;
  }

  public void setInitiatedById(Long initiatedById) {
    this.initiatedById = initiatedById;
  }

  public InventoryTransactionSearchCriteria toCriteria(List<Long> headquarterIds) {
    return new InventoryTransactionSearchCriteria(type, status, fromDate, toDate, initiatedById, headquarterIds);
  }
}
