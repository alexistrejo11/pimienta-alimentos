package io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.mapper;

import io.github.alexistrejo11.pimienta.module.pos.core.application.PosProductReportRow;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.CreateEnrollmentCodeCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.CreatePosOperatorCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.EnrollDeviceCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.UpdatePosOperatorCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosEnrollmentCode;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosOperator;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleLineType;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncIncident;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.DeviceAuthUseCases.DeviceMeResult;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.DeviceAuthUseCases.EnrollResult;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.DeviceAuthUseCases.SiteSummary;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.BootstrapSnapshot;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.OperatorRow;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncBootstrapUseCases.ProductRow;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases.ChangeOperation;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases.ChangesBatch;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases.OperatorData;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases.PoliciesData;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncChangesUseCases.ProductData;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.DeviceTokenIssuer.DeviceIssuedTokens;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.CreateEnrollmentCodeRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.CreatePosOperatorRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceEnrollRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceEnrollResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceMeResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.DeviceTokenPairResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.EnrollmentCodeResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapCursorsResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapDeviceResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapOperatorResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapPoliciesResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapProductResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosBootstrapSiteResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosDeviceAdminResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosLedgerEventReportResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosOperatorResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosProductReportResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSaleReportResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSiteResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncChangeOperationResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncChangesResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncIncidentResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncPoliciesDataResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.UpdatePosOperatorRequest;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.IngestPosEventItem;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.SaleConfirmedPayload;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.SaleLinePayload;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.SalePaymentPayload;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncEventsUseCases.EventIngestResult;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsRequest.PosSyncEventEnvelopeRequest;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsResponse;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.inbound.web.dto.PosSyncEventsResponse.PosSyncEventResultResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PosWebMapper {

  private static final Map<String, Integer> DEFAULT_SCHEMAS =
      Map.of("SALE_CONFIRMED", 1, "SHIFT_CLOSED", 1);

  private static final ObjectMapper JSON =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private PosWebMapper() {}

  public static IngestPosEventsCommand toIngestEventsCommand(PosSyncEventsRequest request) {
    List<IngestPosEventItem> items = new ArrayList<>();
    for (PosSyncEventEnvelopeRequest env : request.events()) {
      String payloadJson = writePayloadJson(env.payload());
      SaleConfirmedPayload sale =
          "SALE_CONFIRMED".equals(env.eventType())
              ? parseSaleConfirmed(env.payload())
              : null;
      items.add(
          new IngestPosEventItem(
              env.eventId(),
              env.eventType(),
              env.schemaVersion() != null ? env.schemaVersion() : 1,
              env.deviceId(),
              env.siteId(),
              env.deviceSequence() != null ? env.deviceSequence() : 0L,
              env.aggregateId(),
              env.shiftId(),
              env.occurredAt(),
              payloadJson,
              sale));
    }
    return new IngestPosEventsCommand(List.copyOf(items));
  }

  public static PosSyncEventsResponse toSyncEventsResponse(List<EventIngestResult> results) {
    return new PosSyncEventsResponse(
        results.stream()
            .map(
                r ->
                    new PosSyncEventResultResponse(
                        r.eventId(),
                        r.status().name(),
                        r.serverReceivedAt(),
                        r.incidentId(),
                        r.message()))
            .toList());
  }

  private static String writePayloadJson(Map<String, Object> payload) {
    try {
      return JSON.writeValueAsString(payload != null ? payload : Map.of());
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  @SuppressWarnings("unchecked")
  private static SaleConfirmedPayload parseSaleConfirmed(Map<String, Object> payload) {
    if (payload == null) {
      return null;
    }
    UUID saleId = parseUuid(payload.get("saleId"));
    if (saleId == null) {
      return null;
    }
    List<SaleLinePayload> lines = new ArrayList<>();
    Object rawLines = payload.get("lines");
    if (rawLines instanceof List<?> list) {
      for (Object o : list) {
        if (o instanceof Map<?, ?> m) {
          Map<String, Object> line = (Map<String, Object>) m;
          lines.add(
              new SaleLinePayload(
                   parseUuid(line.get("lineId")),
                   parseLineType(line.get("lineType")),
                   parseLong(line.get("productId")),
                  asString(line.get("productName")),
                  asString(line.get("saleCategory")),
                  parseInt(line.get("quantity"), 0),
                  asString(line.get("unit")),
                  parseLongPrimitive(line.get("unitPriceCentavos"), 0L),
                  parseLongPrimitive(line.get("subtotalCentavos"), 0L),
                  asString(line.get("stockPolicy")),
                  parseBoolean(line.get("soldWithNegativeStock")),
                  parseBoolean(line.get("soldWhileUnavailable")),
                   asString(line.get("rawBarcode")),
                   parseLong(line.get("authorizedByOperatorId")),
                   parseInstant(line.get("authorizedAt"))));
        }
      }
    }
    List<SalePaymentPayload> payments = new ArrayList<>();
    Object rawPayments = payload.get("payments");
    if (rawPayments instanceof List<?> list) {
      for (Object o : list) {
        if (o instanceof Map<?, ?> m) {
          Map<String, Object> payment = (Map<String, Object>) m;
          payments.add(
              new SalePaymentPayload(
                  parseUuid(payment.get("paymentId")),
                  asString(payment.get("method")),
                  parseLongPrimitive(payment.get("amountCentavos"), 0L),
                  parseLong(payment.get("tenderedCentavos")),
                  parseLong(payment.get("changeCentavos"))));
        }
      }
    }
    return new SaleConfirmedPayload(
        saleId,
        asString(payload.get("folio")),
        parseLong(payload.get("cashierOperatorId")),
        parseLongPrimitive(payload.get("grossCentavos"), 0L),
        parseLongPrimitive(payload.get("discountCentavos"), 0L),
        parseLongPrimitive(payload.get("totalCentavos"), 0L),
        asString(payload.get("status")),
        List.copyOf(lines),
        List.copyOf(payments));
  }

  private static UUID parseUuid(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return UUID.fromString(String.valueOf(value));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static PosSaleLineType parseLineType(Object value) {
    if (value == null) {
      return PosSaleLineType.CATALOG;
    }
    try {
      return PosSaleLineType.valueOf(String.valueOf(value).strip());
    } catch (IllegalArgumentException ex) {
      return PosSaleLineType.CATALOG;
    }
  }

  private static Instant parseInstant(Object value) {
    if (value == null) {
      return null;
    }
    try {
      return Instant.parse(String.valueOf(value));
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private static Long parseLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number n) {
      return n.longValue();
    }
    String s = String.valueOf(value).strip();
    if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
      return null;
    }
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static long parseLongPrimitive(Object value, long defaultValue) {
    Long parsed = parseLong(value);
    return parsed != null ? parsed : defaultValue;
  }

  private static int parseInt(Object value, int defaultValue) {
    if (value instanceof Number n) {
      return n.intValue();
    }
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(String.valueOf(value).strip());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private static boolean parseBoolean(Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    return value != null && Boolean.parseBoolean(String.valueOf(value));
  }

  private static String asString(Object value) {
    if (value == null) {
      return null;
    }
    String s = String.valueOf(value);
    return "null".equalsIgnoreCase(s) ? null : s;
  }

  public static EnrollDeviceCommand toEnrollCommand(DeviceEnrollRequest request) {
    return new EnrollDeviceCommand(
        request.enrollmentCode(),
        request.devicePublicId(),
        request.deviceName(),
        request.appVersion());
  }

  public static DeviceEnrollResponse toEnrollResponse(EnrollResult result) {
    DeviceIssuedTokens t = result.tokens();
    PosDevice d = result.device();
    return new DeviceEnrollResponse(
        d.getId().toString(),
        d.getVisibleCode(),
        d.getStatus().name(),
        toSite(result.site()),
        t.accessToken(),
        t.refreshToken(),
        t.accessTokenExpiresInSeconds(),
        t.refreshTokenExpiresInSeconds(),
        t.refreshTokenMaxExpiresInSeconds(),
        d.getMinAppVersion(),
        DEFAULT_SCHEMAS);
  }

  public static DeviceTokenPairResponse toTokenPair(DeviceIssuedTokens tokens) {
    return new DeviceTokenPairResponse(
        tokens.accessToken(),
        tokens.refreshToken(),
        tokens.accessTokenExpiresInSeconds(),
        tokens.refreshTokenExpiresInSeconds(),
        tokens.refreshTokenMaxExpiresInSeconds());
  }

  public static DeviceMeResponse toMeResponse(DeviceMeResult result) {
    PosDevice d = result.device();
    return new DeviceMeResponse(
        d.getId().toString(),
        d.getVisibleCode(),
        d.getStatus().name(),
        d.getDeviceName(),
        toSite(result.site()),
        d.getMinAppVersion(),
        result.eventSchemaVersions());
  }

  public static PosSiteResponse toSite(SiteSummary site) {
    return new PosSiteResponse(
        String.valueOf(site.id()), site.name(), site.address(), site.currency());
  }

  public static CreatePosOperatorCommand toCreateOperatorCommand(CreatePosOperatorRequest request) {
    return new CreatePosOperatorCommand(
        request.displayName(),
        request.posRole(),
        request.pin(),
        request.userId(),
        request.headquarterIds() != null ? request.headquarterIds() : Set.of());
  }

  public static UpdatePosOperatorCommand toUpdateOperatorCommand(UpdatePosOperatorRequest request) {
    return new UpdatePosOperatorCommand(
        request.displayName(),
        request.posRole(),
        request.pin(),
        request.userId(),
        request.active(),
        request.headquarterIds());
  }

  public static PosOperatorResponse toOperatorResponse(PosOperator operator) {
    return new PosOperatorResponse(
        operator.getId(),
        operator.getDisplayName(),
        operator.getPosRole(),
        operator.getUserId(),
        operator.isActive(),
        operator.getHeadquarterIds());
  }

  public static CreateEnrollmentCodeCommand toEnrollmentCommand(CreateEnrollmentCodeRequest request) {
    return new CreateEnrollmentCodeCommand(request.headquarterId());
  }

  public static EnrollmentCodeResponse toEnrollmentResponse(PosEnrollmentCode code) {
    return new EnrollmentCodeResponse(
        code.getId(),
        code.getCode(),
        code.getHeadquarterId(),
        code.getExpiresAt(),
        code.getCreatedAt());
  }

  public static PosDeviceAdminResponse toDeviceAdminResponse(PosDevice device) {
    return new PosDeviceAdminResponse(
        device.getId(),
        device.getHeadquarterId(),
        device.getVisibleCode(),
        device.getDeviceName(),
        device.getAppVersion(),
        device.getStatus(),
        device.getMinAppVersion());
  }

  public static PosSyncIncidentResponse toSyncIncidentResponse(PosSyncIncident incident) {
    return new PosSyncIncidentResponse(
        incident.getId(),
        incident.getEventId(),
        incident.getHeadquarterId(),
        incident.getReasonCode(),
        incident.getDetail(),
        incident.getAcceptedAt(),
        incident.getAcceptedBy(),
        incident.getAcceptLabel(),
        incident.getAcceptNote(),
        incident.getCreatedAt());
  }

  public static PosSaleReportResponse toSaleReportResponse(
      PosSale sale, io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus syncStatus) {
    return new PosSaleReportResponse(
        sale.getId(),
        sale.getEventId(),
        sale.getHeadquarterId(),
        sale.getDeviceId(),
        sale.getShiftId(),
        sale.getCashierOperatorId(),
        sale.getFolio(),
        sale.getGrossCentavos(),
        sale.getDiscountCentavos(),
        sale.getTotalCentavos(),
        sale.getStatus().name(),
        syncStatus != null ? syncStatus.name() : io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus.ACCEPTED.name(),
         sale.getOccurredAt(),
         sale.getLines().stream().anyMatch(line -> line.lineType() == io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleLineType.OPEN_AMOUNT),
         sale.getLines().stream().map(PosSaleReportResponse.PosSaleLineReportResponse::from).toList());
  }

  public static PosProductReportResponse toProductReportResponse(PosProductReportRow row) {
    return new PosProductReportResponse(
        row.productId(),
        row.productName(),
        row.quantitySum(),
        row.subtotalCentavosSum(),
        row.saleCount());
  }

  @SuppressWarnings("unchecked")
  public static PosLedgerEventReportResponse toLedgerEventReportResponse(PosSyncEvent event) {
    Map<String, Object> payload;
    try {
      Object parsed = JSON.readValue(event.getPayloadJson(), Map.class);
      payload = parsed instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    } catch (JsonProcessingException e) {
      payload = Map.of();
    }
    return new PosLedgerEventReportResponse(
        event.getId(),
        event.getEventType(),
        event.getHeadquarterId(),
        event.getDeviceId(),
        event.getShiftId(),
        event.getOccurredAt(),
        payload);
  }

  public static PosBootstrapResponse toBootstrapResponse(BootstrapSnapshot snapshot) {
    return new PosBootstrapResponse(
        snapshot.schemaVersion(),
        snapshot.kind(),
        snapshot.snapshotId(),
        snapshot.generatedAt(),
        new PosBootstrapSiteResponse(
            snapshot.site().id(),
            snapshot.site().name(),
            snapshot.site().address(),
            snapshot.site().currency()),
        new PosBootstrapDeviceResponse(
            snapshot.device().id(),
            snapshot.device().name(),
            snapshot.device().visibleCode(),
            snapshot.device().status().name()),
        snapshot.operators().stream().map(PosWebMapper::toBootstrapOperator).toList(),
        snapshot.products().stream().map(PosWebMapper::toBootstrapProduct).toList(),
        snapshot.openAmountCategories(),
        new PosBootstrapPoliciesResponse(
            snapshot.policies().allowNegativeStock(),
            snapshot.policies().allowOpenProducts(),
            snapshot.policies().defaultNegativeStockLimit(),
            snapshot.policies().staleCatalogWarnHours(),
            snapshot.policies().staleCatalogBlockHours()),
        new PosBootstrapCursorsResponse(snapshot.cursors().changes()));
  }

  public static PosSyncChangesResponse toChangesResponse(ChangesBatch batch) {
    return new PosSyncChangesResponse(
        batch.schemaVersion(),
        batch.nextCursor(),
        batch.operations().stream().map(PosWebMapper::toChangeOperation).toList());
  }

  private static PosSyncChangeOperationResponse toChangeOperation(ChangeOperation op) {
    Object data = op.data();
    if (data instanceof ProductData p) {
      data =
          new PosBootstrapProductResponse(
              p.id(),
              p.sku(),
              p.barcode(),
              p.name(),
              p.saleCategory(),
              p.unit(),
              p.priceCentavos(),
              p.costCentavos(),
              p.available(),
              p.stockQuantity(),
              p.stockMinQuantity(),
              p.stockPolicy().name(),
              p.negativeStockLimit());
    } else if (data instanceof OperatorData o) {
      data =
          new PosBootstrapOperatorResponse(
              o.id(), o.displayName(), o.role().name(), o.pinHash(), o.active());
    } else if (data instanceof PoliciesData pol) {
      data =
          new PosSyncPoliciesDataResponse(
              pol.allowNegativeStock(),
              pol.allowOpenProducts(),
              pol.defaultNegativeStockLimit(),
              pol.staleCatalogWarnHours(),
              pol.staleCatalogBlockHours(),
              pol.openAmountCategories());
    }
    return new PosSyncChangeOperationResponse(op.op(), op.entity(), op.id(), data);
  }

  private static PosBootstrapOperatorResponse toBootstrapOperator(OperatorRow row) {
    return new PosBootstrapOperatorResponse(
        row.id(), row.displayName(), row.role().name(), row.pinHash(), row.active());
  }

  private static PosBootstrapProductResponse toBootstrapProduct(ProductRow row) {
    return new PosBootstrapProductResponse(
        row.id(),
        row.sku(),
        row.barcode(),
        row.name(),
        row.saleCategory(),
        row.unit(),
        row.priceCentavos(),
        row.costCentavos(),
        row.available(),
        row.stockQuantity(),
        row.stockMinQuantity(),
        row.stockPolicy().name(),
        row.negativeStockLimit());
  }
}
