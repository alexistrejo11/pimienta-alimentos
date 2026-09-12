package io.github.alexistrejo11.pimienta.module.pos.core.domain;

import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosPaymentMethod;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStockPolicy;
import io.github.alexistrejo11.pimienta.shared.BaseDomain;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PosSale extends BaseDomain<UUID> {

  private UUID eventId;
  private Long headquarterId;
  private UUID deviceId;
  private UUID shiftId;
  private Long cashierOperatorId;
  private String folio;
  private long grossCentavos;
  private long discountCentavos;
  private long totalCentavos;
  private PosSaleStatus status;
  private Instant occurredAt;
  private List<Line> lines = new ArrayList<>();
  private List<Payment> payments = new ArrayList<>();

  private PosSale() {
    this.eventId = UUID.randomUUID();
    this.headquarterId = 0L;
    this.deviceId = UUID.randomUUID();
    this.folio = "";
    this.status = PosSaleStatus.CONFIRMED;
    this.occurredAt = Instant.now();
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    this.version = 0L;
  }

  public UUID getEventId() {
    return eventId;
  }

  public Long getHeadquarterId() {
    return headquarterId;
  }

  public UUID getDeviceId() {
    return deviceId;
  }

  public UUID getShiftId() {
    return shiftId;
  }

  public Long getCashierOperatorId() {
    return cashierOperatorId;
  }

  public String getFolio() {
    return folio != null ? folio : "";
  }

  public long getGrossCentavos() {
    return grossCentavos;
  }

  public long getDiscountCentavos() {
    return discountCentavos;
  }

  public long getTotalCentavos() {
    return totalCentavos;
  }

  public PosSaleStatus getStatus() {
    return status != null ? status : PosSaleStatus.CONFIRMED;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public List<Line> getLines() {
    return lines != null ? List.copyOf(lines) : List.of();
  }

  public List<Payment> getPayments() {
    return payments != null ? List.copyOf(payments) : List.of();
  }

  public record Line(
      UUID lineId,
      Long productId,
      String productName,
      String saleCategory,
      int quantity,
      String unit,
      long unitPriceCentavos,
      long subtotalCentavos,
      PosSaleStockPolicy stockPolicy,
      boolean soldWithNegativeStock,
      boolean soldWhileUnavailable,
      String rawBarcode) {}

  public record Payment(
      UUID paymentId,
      PosPaymentMethod method,
      long amountCentavos,
      Long tenderedCentavos,
      Long changeCentavos) {}

  public static SafeBuilder builder() {
    return new SafeBuilder();
  }

  public static final class SafeBuilder {
    private UUID id;
    private UUID eventId;
    private Long headquarterId;
    private UUID deviceId;
    private UUID shiftId;
    private Long cashierOperatorId;
    private String folio;
    private Long grossCentavos;
    private Long discountCentavos;
    private Long totalCentavos;
    private PosSaleStatus status;
    private Instant occurredAt;
    private List<Line> lines = new ArrayList<>();
    private List<Payment> payments = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Long version;

    public SafeBuilder withId(UUID id) {
      this.id = id;
      return this;
    }

    public SafeBuilder withEventId(UUID eventId) {
      this.eventId = eventId;
      return this;
    }

    public SafeBuilder withHeadquarterId(Long headquarterId) {
      this.headquarterId = headquarterId;
      return this;
    }

    public SafeBuilder withDeviceId(UUID deviceId) {
      this.deviceId = deviceId;
      return this;
    }

    public SafeBuilder withShiftId(UUID shiftId) {
      this.shiftId = shiftId;
      return this;
    }

    public SafeBuilder withCashierOperatorId(Long cashierOperatorId) {
      this.cashierOperatorId = cashierOperatorId;
      return this;
    }

    public SafeBuilder withFolio(String folio) {
      this.folio = folio;
      return this;
    }

    public SafeBuilder withGrossCentavos(Long grossCentavos) {
      this.grossCentavos = grossCentavos;
      return this;
    }

    public SafeBuilder withDiscountCentavos(Long discountCentavos) {
      this.discountCentavos = discountCentavos;
      return this;
    }

    public SafeBuilder withTotalCentavos(Long totalCentavos) {
      this.totalCentavos = totalCentavos;
      return this;
    }

    public SafeBuilder withStatus(PosSaleStatus status) {
      this.status = status;
      return this;
    }

    public SafeBuilder withOccurredAt(Instant occurredAt) {
      this.occurredAt = occurredAt;
      return this;
    }

    public SafeBuilder withLines(List<Line> lines) {
      this.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
      return this;
    }

    public SafeBuilder withPayments(List<Payment> payments) {
      this.payments = payments != null ? new ArrayList<>(payments) : new ArrayList<>();
      return this;
    }

    public SafeBuilder withCreatedAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SafeBuilder withUpdatedAt(LocalDateTime updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public SafeBuilder withDeletedAt(LocalDateTime deletedAt) {
      this.deletedAt = deletedAt;
      return this;
    }

    public SafeBuilder withVersion(Long version) {
      this.version = version;
      return this;
    }

    public PosSale reconstruct() {
      PosSale s = new PosSale();
      s.id = id;
      s.eventId = eventId != null ? eventId : UUID.randomUUID();
      s.headquarterId = headquarterId != null ? headquarterId : 0L;
      s.deviceId = deviceId != null ? deviceId : UUID.randomUUID();
      s.shiftId = shiftId;
      s.cashierOperatorId = cashierOperatorId;
      s.folio = folio != null ? folio : "";
      s.grossCentavos = grossCentavos != null ? grossCentavos : 0L;
      s.discountCentavos = discountCentavos != null ? discountCentavos : 0L;
      s.totalCentavos = totalCentavos != null ? totalCentavos : 0L;
      s.status = status != null ? status : PosSaleStatus.CONFIRMED;
      s.occurredAt = occurredAt != null ? occurredAt : Instant.now();
      s.lines = lines != null ? new ArrayList<>(lines) : new ArrayList<>();
      s.payments = payments != null ? new ArrayList<>(payments) : new ArrayList<>();
      s.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
      s.updatedAt = updatedAt != null ? updatedAt : s.createdAt;
      s.deletedAt = deletedAt;
      s.version = version != null ? version : 0L;
      return s;
    }

    public PosSale register() {
      PosSale s = reconstruct();
      if (s.id == null) {
        throw new IllegalArgumentException("saleId is required");
      }
      LocalDateTime now = LocalDateTime.now();
      s.createdAt = now;
      s.updatedAt = now;
      s.version = null;
      return s;
    }
  }
}
