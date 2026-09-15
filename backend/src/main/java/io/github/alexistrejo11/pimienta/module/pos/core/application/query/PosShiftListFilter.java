package io.github.alexistrejo11.pimienta.module.pos.core.application.query;

import java.time.Instant;

public record PosShiftListFilter(
    Instant rangeFrom,
    Instant rangeTo,
    String status,
    Long cashierOperatorId) {

  /** When status is CLOSED, or legacy list with dates and no status (filters closedAt). */
  public boolean filterClosedDates() {
    return "CLOSED".equals(status)
        || (status == null && (rangeFrom != null || rangeTo != null));
  }

  public boolean filterOpenedDates() {
    return "OPEN".equals(status);
  }

  public Instant closedFrom() {
    return filterClosedDates() ? rangeFrom : null;
  }

  public Instant closedTo() {
    return filterClosedDates() ? rangeTo : null;
  }

  public Instant openedFrom() {
    return filterOpenedDates() ? rangeFrom : null;
  }

  public Instant openedTo() {
    return filterOpenedDates() ? rangeTo : null;
  }

  public boolean hasDateOrStatusFilter() {
    return status != null
        || cashierOperatorId != null
        || rangeFrom != null
        || rangeTo != null;
  }
}
