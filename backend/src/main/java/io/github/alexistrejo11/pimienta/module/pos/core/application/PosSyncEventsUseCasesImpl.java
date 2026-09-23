package io.github.alexistrejo11.pimienta.module.pos.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.ApplyPosSaleStockCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosSaleInventoryUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.IngestPosEventItem;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.SaleConfirmedPayload;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.SaleLinePayload;
import io.github.alexistrejo11.pimienta.module.pos.core.application.command.IngestPosEventsCommand.SalePaymentPayload;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosDevice;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSale;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.PosSyncEvent;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosEventResultStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosPaymentMethod;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStatus;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.enums.PosSaleStockPolicy;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceNotFoundException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosDeviceRevokedException;
import io.github.alexistrejo11.pimienta.module.pos.core.domain.exception.PosShiftMaterializationException;
import io.github.alexistrejo11.pimienta.module.pos.core.port.input.PosSyncEventsUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosDeviceRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSaleRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosSyncEventRepository;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosShiftRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class PosSyncEventsUseCasesImpl implements PosSyncEventsUseCases {

  private static final Logger log = LoggerFactory.getLogger(PosSyncEventsUseCasesImpl.class);
  private static final String SALE_CONFIRMED = "SALE_CONFIRMED";
  private static final Set<String> SHIFT_EVENTS = Set.of(
      "SHIFT_OPENED", "CASH_WITHDRAWAL_RECORDED", "CASH_DEPOSIT_RECORDED", "CASH_COUNT_SUBMITTED", "SHIFT_CLOSED");

  private final PosDeviceRepository deviceRepository;
  private final PosSyncEventRepository syncEventRepository;
  private final PosSaleRepository saleRepository;
  private final HeadquarterItemRepository headquarterItemRepository;
  private final PosSaleInventoryUseCases posSaleInventoryUseCases;
  private final PosEventStockProjector eventStockProjector;
  private final TransactionTemplate transactionTemplate;
  private final PosShiftRepository shiftRepository;

  public PosSyncEventsUseCasesImpl(
      PosDeviceRepository deviceRepository,
      PosSyncEventRepository syncEventRepository,
      PosSaleRepository saleRepository,
      HeadquarterItemRepository headquarterItemRepository,
      PosSaleInventoryUseCases posSaleInventoryUseCases,
      PosEventStockProjector eventStockProjector,
      PlatformTransactionManager transactionManager,
      PosShiftRepository shiftRepository) {
    this.deviceRepository = deviceRepository;
    this.syncEventRepository = syncEventRepository;
    this.saleRepository = saleRepository;
    this.headquarterItemRepository = headquarterItemRepository;
    this.posSaleInventoryUseCases = posSaleInventoryUseCases;
    this.eventStockProjector = eventStockProjector;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.shiftRepository = shiftRepository;
  }

  @Override
  public List<EventIngestResult> ingest(UUID authenticatedDeviceId, IngestPosEventsCommand command) {
    PosDevice device =
        deviceRepository
            .findById(authenticatedDeviceId)
            .orElseThrow(() -> new PosDeviceNotFoundException(authenticatedDeviceId));
    if (device.isRevoked()) {
      throw new PosDeviceRevokedException(authenticatedDeviceId);
    }

    List<IngestPosEventItem> ordered =
        command.events() == null
            ? List.of()
            : command.events().stream()
                .sorted(Comparator.comparingLong(IngestPosEventItem::deviceSequence))
                .toList();

    List<EventIngestResult> results = new ArrayList<>(ordered.size());
    for (IngestPosEventItem item : ordered) {
      EventIngestResult result;
      try {
        result =
            Objects.requireNonNull(
                transactionTemplate.execute(status -> processOne(device, item)));
      } catch (DataIntegrityViolationException ex) {
        // Concurrent insert raced past the pre-check; classify without leaving a 500.
        result = resolveIntegrityConflict(device.getId(), item);
      }
      results.add(result);
    }
    return List.copyOf(results);
  }

  private EventIngestResult resolveIntegrityConflict(UUID deviceId, IngestPosEventItem item) {
    Optional<PosSyncEvent> byEventId = syncEventRepository.findByEventId(item.eventId());
    if (byEventId.isPresent()) {
      PosSyncEvent prior = byEventId.get();
      return new EventIngestResult(
          prior.getId(),
          PosEventResultStatus.DUPLICATE,
          prior.getServerReceivedAt(),
          prior.getIncidentId(),
          "Event already processed");
    }
    Optional<PosSyncEvent> bySequence =
        syncEventRepository.findByDeviceIdAndDeviceSequence(deviceId, item.deviceSequence());
    if (bySequence.isPresent()) {
      PosSyncEvent prior = bySequence.get();
      return new EventIngestResult(
          item.eventId(),
          PosEventResultStatus.REJECTED,
          Instant.now(),
          null,
          "deviceSequence "
              + item.deviceSequence()
              + " already used by eventId="
              + prior.getId()
              + "; continue from a higher sequence after re-enrollment");
    }
    throw new IllegalStateException(
        "POS sync integrity conflict without matching eventId or deviceSequence for eventId="
            + item.eventId());
  }

  private EventIngestResult processOne(PosDevice device, IngestPosEventItem item) {
    Optional<PosSyncEvent> existing = syncEventRepository.findByEventId(item.eventId());
    if (existing.isPresent()) {
      PosSyncEvent prior = existing.get();
      return new EventIngestResult(
          prior.getId(),
          PosEventResultStatus.DUPLICATE,
          prior.getServerReceivedAt(),
          prior.getIncidentId(),
          "Event already processed");
    }

    Optional<PosSyncEvent> sequenceTaken =
        syncEventRepository.findByDeviceIdAndDeviceSequence(device.getId(), item.deviceSequence());
    if (sequenceTaken.isPresent()) {
      PosSyncEvent prior = sequenceTaken.get();
      return new EventIngestResult(
          item.eventId(),
          PosEventResultStatus.REJECTED,
          Instant.now(),
          null,
          "deviceSequence "
              + item.deviceSequence()
              + " already used by eventId="
              + prior.getId()
              + "; continue from a higher sequence after re-enrollment");
    }

    if (!device.getId().equals(item.deviceId())
        || !String.valueOf(device.getHeadquarterId()).equals(item.siteId())) {
      Instant now = Instant.now();
      return new EventIngestResult(
          item.eventId(),
          PosEventResultStatus.REJECTED,
          now,
          null,
          "siteId or deviceId does not match authenticated device");
    }

    if (SALE_CONFIRMED.equals(item.eventType())) {
      return processSaleConfirmed(device, item);
    }

    if (SHIFT_EVENTS.contains(item.eventType()) && !validShiftEvent(item)) {
      return new EventIngestResult(
          item.eventId(),
          PosEventResultStatus.REJECTED,
          Instant.now(),
          null,
          "required shift event identifiers or payload fields are missing");
    }

    PosSyncEvent event =
        PosSyncEvent.builder()
            .withId(item.eventId())
            .withEventType(item.eventType())
            .withSchemaVersion(item.schemaVersion())
            .withDeviceId(device.getId())
            .withHeadquarterId(device.getHeadquarterId())
            .withDeviceSequence(item.deviceSequence())
            .withAggregateId(item.aggregateId())
            .withShiftId(item.shiftId())
            .withOccurredAt(item.occurredAt() != null ? item.occurredAt() : Instant.now())
            .withPayloadJson(item.payloadJson() != null ? item.payloadJson() : "{}")
            .withStatus(PosEventResultStatus.ACCEPTED)
            .withMessage(null)
            .register();
    PosSyncEvent saved = syncEventRepository.save(event);
    if (SHIFT_EVENTS.contains(item.eventType())) {
      try {
        shiftRepository.materialize(saved);
      } catch (PosShiftMaterializationException ex) {
        saved.markRejected(ex.getMessage());
        PosSyncEvent rejected = syncEventRepository.save(saved);
        bumpDeviceSequence(device, item.deviceSequence());
        return new EventIngestResult(
            rejected.getId(),
            PosEventResultStatus.REJECTED,
            rejected.getServerReceivedAt(),
            null,
            ex.getMessage());
      }
    }
    eventStockProjector.project(
        device.getHeadquarterId(),
        saved.getId(),
        item.eventType(),
        item.aggregateId(),
        item.payloadJson());
    bumpDeviceSequence(device, item.deviceSequence());
    return new EventIngestResult(
        saved.getId(),
        PosEventResultStatus.ACCEPTED,
        saved.getServerReceivedAt(),
        null,
        null);
  }

  private static boolean validShiftEvent(IngestPosEventItem item) {
    if (item.shiftId() == null || item.aggregateId() == null || item.payloadJson() == null
        || item.payloadJson().isBlank() || "{}".equals(item.payloadJson())) {
      return false;
    }
    return switch (item.eventType()) {
      case "SHIFT_OPENED" -> item.payloadJson().contains("shiftId")
          && item.payloadJson().contains("openingCashCentavos");
      case "CASH_WITHDRAWAL_RECORDED", "CASH_DEPOSIT_RECORDED" -> item.payloadJson().contains("amountCentavos");
      case "CASH_COUNT_SUBMITTED" -> item.payloadJson().contains("totalCentavos")
          && item.payloadJson().contains("denominations");
      case "SHIFT_CLOSED" -> item.payloadJson().contains("cashExpectedCentavos")
          && item.payloadJson().contains("countedCashCentavos")
          && item.payloadJson().contains("differenceCentavos");
      default -> false;
    };
  }

  private EventIngestResult processSaleConfirmed(PosDevice device, IngestPosEventItem item) {
    SaleConfirmedPayload payload = item.saleConfirmed();
    if (payload == null || payload.saleId() == null) {
      Instant now = Instant.now();
      return new EventIngestResult(
          item.eventId(),
          PosEventResultStatus.REJECTED,
          now,
          null,
          "SALE_CONFIRMED payload.saleId is required");
    }

    // If sale already exists under another event, do not overwrite / re-stock.
    Optional<PosSale> existingSale = saleRepository.findBySaleId(payload.saleId());
    if (existingSale.isPresent()
        && !existingSale.get().getEventId().equals(item.eventId())) {
      return persistSaleIdConflict(device, item, payload);
    }

    PosSyncEvent event =
        PosSyncEvent.builder()
            .withId(item.eventId())
            .withEventType(SALE_CONFIRMED)
            .withSchemaVersion(item.schemaVersion())
            .withDeviceId(device.getId())
            .withHeadquarterId(device.getHeadquarterId())
            .withDeviceSequence(item.deviceSequence())
            .withAggregateId(item.aggregateId() != null ? item.aggregateId() : payload.saleId())
            .withShiftId(item.shiftId())
            .withOccurredAt(item.occurredAt() != null ? item.occurredAt() : Instant.now())
            .withPayloadJson(item.payloadJson() != null ? item.payloadJson() : "{}")
            .withStatus(PosEventResultStatus.ACCEPTED)
            .register();
    PosSyncEvent savedEvent = syncEventRepository.save(event);

    PosSale sale = buildSale(device, item, payload);
    saleRepository.save(sale);

    applyControlledStock(device.getHeadquarterId(), item.eventId(), payload);

    bumpDeviceSequence(device, item.deviceSequence());
    log.info(
        "POS SALE_CONFIRMED eventId={} saleId={} status={}",
        item.eventId(),
        payload.saleId(),
        PosEventResultStatus.ACCEPTED);
    return new EventIngestResult(
        savedEvent.getId(),
        PosEventResultStatus.ACCEPTED,
        savedEvent.getServerReceivedAt(),
        null,
        null);
  }

  private EventIngestResult persistSaleIdConflict(
      PosDevice device, IngestPosEventItem item, SaleConfirmedPayload payload) {
    String message =
        "saleId already exists under a different eventId; sale was not overwritten";
    PosSyncEvent event =
        PosSyncEvent.builder()
            .withId(item.eventId())
            .withEventType(SALE_CONFIRMED)
            .withSchemaVersion(item.schemaVersion())
            .withDeviceId(device.getId())
            .withHeadquarterId(device.getHeadquarterId())
            .withDeviceSequence(item.deviceSequence())
            .withAggregateId(item.aggregateId() != null ? item.aggregateId() : payload.saleId())
            .withShiftId(item.shiftId())
            .withOccurredAt(item.occurredAt() != null ? item.occurredAt() : Instant.now())
            .withPayloadJson(item.payloadJson() != null ? item.payloadJson() : "{}")
            .withStatus(PosEventResultStatus.REJECTED)
            .withMessage(message)
            .register();
    PosSyncEvent savedEvent = syncEventRepository.save(event);
    bumpDeviceSequence(device, item.deviceSequence());
    return new EventIngestResult(
        savedEvent.getId(),
        PosEventResultStatus.REJECTED,
        savedEvent.getServerReceivedAt(),
        null,
        message);
  }

  private PosSale buildSale(
      PosDevice device, IngestPosEventItem item, SaleConfirmedPayload payload) {
    List<PosSale.Line> lines = new ArrayList<>();
    if (payload.lines() != null) {
      for (SaleLinePayload line : payload.lines()) {
        lines.add(
            new PosSale.Line(
                line.lineId() != null ? line.lineId() : UUID.randomUUID(),
                line.lineType(),
                line.productId(),
                line.productName() != null ? line.productName() : "",
                line.saleCategory(),
                line.quantity(),
                line.unit() != null ? line.unit() : "PIECE",
                line.unitPriceCentavos(),
                line.subtotalCentavos(),
                parseStockPolicy(line.stockPolicy()),
                line.soldWithNegativeStock(),
                line.soldWhileUnavailable(),
                blankToNull(line.rawBarcode()),
                line.authorizedByOperatorId(),
                line.authorizedAt()));
      }
    }
    List<PosSale.Payment> payments = new ArrayList<>();
    if (payload.payments() != null) {
      for (SalePaymentPayload payment : payload.payments()) {
        payments.add(
            new PosSale.Payment(
                payment.paymentId() != null ? payment.paymentId() : UUID.randomUUID(),
                parsePaymentMethod(payment.method()),
                payment.amountCentavos(),
                payment.tenderedCentavos(),
                payment.changeCentavos()));
      }
    }
    return PosSale.builder()
        .withId(payload.saleId())
        .withEventId(item.eventId())
        .withHeadquarterId(device.getHeadquarterId())
        .withDeviceId(device.getId())
        .withShiftId(item.shiftId())
        .withCashierOperatorId(payload.cashierOperatorId())
        .withFolio(payload.folio() != null ? payload.folio() : "")
        .withGrossCentavos(payload.grossCentavos())
        .withDiscountCentavos(payload.discountCentavos())
        .withTotalCentavos(payload.totalCentavos())
        .withStatus(parseSaleStatus(payload.status()))
        .withOccurredAt(item.occurredAt() != null ? item.occurredAt() : Instant.now())
        .withLines(lines)
        .withPayments(payments)
        .register();
  }

  private void applyControlledStock(
      long headquarterId, UUID eventId, SaleConfirmedPayload payload) {
    if (payload.lines() == null) {
      return;
    }
    for (SaleLinePayload line : payload.lines()) {
      if (line.productId() == null) {
        continue;
      }
      Optional<HeadquarterItem> catalog =
          headquarterItemRepository.findByHeadquarterIdAndItemId(headquarterId, line.productId());
      if (catalog.isEmpty()
          || catalog.get().getStockPolicy() != HeadquarterItem.StockPolicy.CONTROLLED) {
        continue;
      }
      if (line.quantity() == 0) {
        continue;
      }
      posSaleInventoryUseCases.applySaleStock(
          new ApplyPosSaleStockCommand(
              headquarterId,
              line.productId(),
              line.quantity(),
              eventId.toString(),
              null,
              null));
    }
  }

  private void bumpDeviceSequence(PosDevice device, long sequence) {
    PosDevice current =
        deviceRepository
            .findById(device.getId())
            .orElseThrow(() -> new PosDeviceNotFoundException(device.getId()));
    current.recordDeviceSequence(sequence);
    deviceRepository.save(current);
  }

  private static PosSaleStockPolicy parseStockPolicy(String value) {
    if (value == null || value.isBlank()) {
      return PosSaleStockPolicy.NOT_CONTROLLED;
    }
    try {
      return PosSaleStockPolicy.valueOf(value.strip());
    } catch (IllegalArgumentException ex) {
      return PosSaleStockPolicy.NOT_CONTROLLED;
    }
  }

  private static PosPaymentMethod parsePaymentMethod(String value) {
    if (value == null || value.isBlank()) {
      return PosPaymentMethod.CASH;
    }
    try {
      return PosPaymentMethod.valueOf(value.strip());
    } catch (IllegalArgumentException ex) {
      return PosPaymentMethod.CASH;
    }
  }

  private static PosSaleStatus parseSaleStatus(String value) {
    if (value == null || value.isBlank()) {
      return PosSaleStatus.CONFIRMED;
    }
    try {
      return PosSaleStatus.valueOf(value.strip());
    } catch (IllegalArgumentException ex) {
      return PosSaleStatus.CONFIRMED;
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
