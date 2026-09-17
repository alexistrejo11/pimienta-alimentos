package io.github.alexistrejo11.pimienta.module.pos.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.ApplyPosSaleStockCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.domain.Inventory;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosLocationUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosSaleInventoryUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.output.InventoryRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class PosSyncEventsIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private PosSaleInventoryUseCases posSaleInventoryUseCases;
  @Autowired private PosLocationUseCases posLocationUseCases;
  @Autowired private InventoryRepository inventoryRepository;

  @Test
  void saleConfirmed_acceptedThenDuplicate_stockAppliedOnce() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B4-HQ-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-B4-" + UUID.randomUUID(), "Refresco");
    putCatalog(staffToken, hqId, itemId, "Bebidas", "25.00", "CONTROLLED");
    long operatorId = createOperator(staffToken, hqId, "Cajera B4");

    EnrolledDevice device = enrollDevice(staffToken, hqId, "Caja B4");
    posLocationUseCases.ensurePosLocation(hqId);
    posSaleInventoryUseCases.applySaleStock(
        new ApplyPosSaleStockCommand(hqId, itemId, -10, "seed", null, null));

    UUID eventId = UUID.randomUUID();
    UUID saleId = UUID.randomUUID();
    String body = saleEventJson(device, hqId, eventId, saleId, itemId, operatorId, 1, 2500, false);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer("/api/v1/pos/sync/events", device.accessToken(), body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results", hasSize(1)))
        .andExpect(jsonPath("$.results[0].eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"))
        .andExpect(jsonPath("$.results[0].incidentId", nullValue()));

    Inventory afterFirst = stockAtPos(hqId, itemId);
    org.junit.jupiter.api.Assertions.assertEquals(9, afterFirst.getAvailableQuantity());

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer("/api/v1/pos/sync/events", device.accessToken(), body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("DUPLICATE"))
        .andExpect(jsonPath("$.results[0].message").value("Event already processed"));

    Inventory afterDup = stockAtPos(hqId, itemId);
    org.junit.jupiter.api.Assertions.assertEquals(9, afterDup.getAvailableQuantity());
  }

  @Test
  void saleConfirmed_allowsNegativeStockOnPosLocation() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B4-NEG-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-NEG-" + UUID.randomUUID(), "Cafe");
    putCatalog(staffToken, hqId, itemId, "Deli", "40.00", "CONTROLLED");
    long operatorId = createOperator(staffToken, hqId, "Cajera Neg");

    EnrolledDevice device = enrollDevice(staffToken, hqId, "Caja Neg");
    // no seed stock — sale drives quantity negative
    UUID eventId = UUID.randomUUID();
    UUID saleId = UUID.randomUUID();
    String body = saleEventJson(device, hqId, eventId, saleId, itemId, operatorId, 3, 4000, true);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer("/api/v1/pos/sync/events", device.accessToken(), body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("REQUIRES_REVIEW"))
        .andExpect(jsonPath("$.results[0].incidentId").isNotEmpty());

    Inventory inv = stockAtPos(hqId, itemId);
    org.junit.jupiter.api.Assertions.assertEquals(-3, inv.getAvailableQuantity());
  }

  @Test
  void saleConfirmed_siteMismatch_rejected() throws Exception {
    String staffToken = obtainAccessToken();
    long hqA = createHeadquarter(staffToken, "POS-B4-A-" + UUID.randomUUID());
    long hqB = createHeadquarter(staffToken, "POS-B4-B-" + UUID.randomUUID());
    putPosSettings(staffToken, hqA);
    long itemId = createItem(staffToken, "SKU-MIS-" + UUID.randomUUID(), "Item");
    putCatalog(staffToken, hqA, itemId, "Otros", "10.00", "CONTROLLED");
    long operatorId = createOperator(staffToken, hqA, "Cajera Mis");

    EnrolledDevice device = enrollDevice(staffToken, hqA, "Caja Mis");
    UUID eventId = UUID.randomUUID();
    UUID saleId = UUID.randomUUID();
    String body = saleEventJson(device, hqB, eventId, saleId, itemId, operatorId, 1, 1000, false);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer("/api/v1/pos/sync/events", device.accessToken(), body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("REJECTED"))
        .andExpect(jsonPath("$.results[0].incidentId", nullValue()));
  }

  @Test
  void saleConfirmed_revokedDevice_returns403() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B4-REV-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-REV-" + UUID.randomUUID(), "Item");
    putCatalog(staffToken, hqId, itemId, "Otros", "10.00", "NOT_CONTROLLED");
    long operatorId = createOperator(staffToken, hqId, "Cajera Rev");

    UUID devicePublicId = UUID.randomUUID();
    String code = createEnrollmentCode(staffToken, hqId);
    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll", enrollJson(code, devicePublicId, "Rev")))
            .andExpect(status().isOk())
            .andReturn();
    String access = JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");
    String deviceId = JsonPath.read(enroll.getResponse().getContentAsString(), "$.deviceId");

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/pos/admin/devices/" + deviceId + "/revoke", staffToken))
        .andExpect(status().isOk());

    String body =
        saleEventJson(
            new EnrolledDevice(UUID.fromString(deviceId), access),
            hqId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            itemId,
            operatorId,
            1,
            1000,
            false);

    mockMvc
        .perform(AccountTestRequests.postJsonBearer("/api/v1/pos/sync/events", access, body))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("POS_DEVICE_REVOKED"));
  }

  @Test
  void shiftOpenedThenClosed_materializesShiftForAdminList() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-SHIFT-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long operatorId = createOperator(staffToken, hqId, "Cajera Shift");
    EnrolledDevice device = enrollDevice(staffToken, hqId, "Caja Shift");
    UUID shiftId = UUID.randomUUID();
    UUID openEventId = UUID.randomUUID();
    UUID closeEventId = UUID.randomUUID();
    UUID closeAggregateId = UUID.randomUUID();

    String openBatch =
        shiftLifecycleEventJson(
            device,
            hqId,
            shiftId,
            openEventId,
            1L,
            "SHIFT_OPENED",
            shiftId,
            """
            {
              "shiftId": "%s",
              "cashierOperatorId": %d,
              "openingCashCentavos": 50000
            }
            """
                .formatted(shiftId, operatorId));

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events", device.accessToken(), openBatch))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/pos/admin/shifts?headquarterId=%d&status=OPEN".formatted(hqId),
                staffToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].shiftId").value(shiftId.toString()));

    String closeBatch =
        shiftLifecycleEventJson(
            device,
            hqId,
            shiftId,
            closeEventId,
            2L,
            "SHIFT_CLOSED",
            closeAggregateId,
            """
            {
              "cashExpectedCentavos": 60000,
              "countedCashCentavos": 60000,
              "differenceCentavos": 0,
              "approvedByUserId": "%d"
            }
            """
                .formatted(operatorId));

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events", device.accessToken(), closeBatch))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/pos/admin/shifts?headquarterId=%d&status=CLOSED&from=2026-09-01T00:00:00Z&to=2026-09-30T00:00:00Z"
                    .formatted(hqId),
                staffToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].shiftId").value(shiftId.toString()))
        .andExpect(jsonPath("$.items[0].openingCashCentavos").value(50000))
        .andExpect(jsonPath("$.items[0].expectedCashCentavos").value(60000))
        .andExpect(jsonPath("$.items[0].cashierDisplayName").value("Cajera Shift"));
  }

  @Test
  void shiftReconciliation_aggregatesAcceptedSalesAndWithdrawal() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-RECON-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-RECON-" + UUID.randomUUID(), "Snack");
    putCatalog(staffToken, hqId, itemId, "Deli", "25.00", "CONTROLLED");
    long operatorId = createOperator(staffToken, hqId, "Cajera Recon");
    EnrolledDevice device = enrollDevice(staffToken, hqId, "Caja Recon");
    UUID shiftId = UUID.randomUUID();

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events",
                device.accessToken(),
                shiftLifecycleEventJson(
                    device,
                    hqId,
                    shiftId,
                    UUID.randomUUID(),
                    1L,
                    "SHIFT_OPENED",
                    shiftId,
                    """
                    {"shiftId": "%s", "cashierOperatorId": %d, "openingCashCentavos": 10000}
                    """
                        .formatted(shiftId, operatorId))))
        .andExpect(status().isOk());

    UUID cashSaleEvent = UUID.randomUUID();
    UUID cashSaleId = UUID.randomUUID();
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events",
                device.accessToken(),
                saleEventJsonForShift(
                    device,
                    hqId,
                    cashSaleEvent,
                    cashSaleId,
                    shiftId,
                    itemId,
                    operatorId,
                    1,
                    2500,
                    false,
                    "CASH")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"));

    UUID withdrawalEvent = UUID.randomUUID();
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events",
                device.accessToken(),
                shiftLifecycleEventJson(
                    device,
                    hqId,
                    shiftId,
                    withdrawalEvent,
                    3L,
                    "CASH_WITHDRAWAL_RECORDED",
                    UUID.randomUUID(),
                    """
                    {"amountCentavos": 1000, "folio": "S1", "reason": "Sangria"}
                    """)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events",
                device.accessToken(),
                shiftLifecycleEventJson(
                    device,
                    hqId,
                    shiftId,
                    UUID.randomUUID(),
                    4L,
                    "SHIFT_CLOSED",
                    UUID.randomUUID(),
                    """
                    {
                      "cashExpectedCentavos": 11500,
                      "countedCashCentavos": 11500,
                      "differenceCentavos": 0,
                      "approvedByUserId": "%d"
                    }
                    """
                        .formatted(operatorId))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/pos/admin/shifts/%s/reconciliation?headquarterId=%d"
                    .formatted(shiftId, hqId),
                staffToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cashSalesCentavos").value(2500))
        .andExpect(jsonPath("$.withdrawalsCentavos").value(1000))
        .andExpect(jsonPath("$.saleTicketCount").value(1))
        .andExpect(jsonPath("$.openingCashCentavos").value(10000));
  }

  @Test
  void salesReport_listsRequiresReviewSyncStatus() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-SALE-REV-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-REV-REP-" + UUID.randomUUID(), "Snack");
    putCatalog(staffToken, hqId, itemId, "Deli", "40.00", "CONTROLLED");
    long operatorId = createOperator(staffToken, hqId, "Cajera RevRep");
    EnrolledDevice device = enrollDevice(staffToken, hqId, "Caja RevRep");

    UUID eventId = UUID.randomUUID();
    UUID saleId = UUID.randomUUID();
    String body = saleEventJson(device, hqId, eventId, saleId, itemId, operatorId, 2, 4000, true);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer("/api/v1/pos/sync/events", device.accessToken(), body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("REQUIRES_REVIEW"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/pos/admin/reports/sales?headquarterId=%d&from=2026-09-01T00:00:00Z&to=2026-09-30T00:00:00Z"
                    .formatted(hqId),
                staffToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].saleId").value(saleId.toString()))
        .andExpect(jsonPath("$.items[0].syncStatus").value("REQUIRES_REVIEW"));
  }

  @Test
  void openAmount_enabledWithManager_isAuditedAndDoesNotMoveInventory() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-OPEN-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId, true);
    long itemId = createItem(staffToken, "SKU-OPEN-" + UUID.randomUUID(), "Open seed");
    long managerId = createOperator(staffToken, hqId, "Manager Open", "MANAGER");
    EnrolledDevice device = enrollDevice(staffToken, hqId, "Caja Open");

    String body = openSaleEventJson(device, hqId, UUID.randomUUID(), UUID.randomUUID(), itemId, managerId);
    mockMvc.perform(AccountTestRequests.postJsonBearer("/api/v1/pos/sync/events", device.accessToken(), body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("REQUIRES_REVIEW"))
        .andExpect(jsonPath("$.results[0].message").value(org.hamcrest.Matchers.containsString("OPEN_PRODUCT")));
  }

  @Test
  void reusedDeviceSequence_differentEventId_returnsRejected() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-SEQ-CONFLICT-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    EnrolledDevice device = enrollDevice(staffToken, hqId, "Caja Seq");

    UUID firstEventId = UUID.randomUUID();
    String first =
        """
        {
          "events": [
            {
              "eventId": "%s",
              "eventType": "DEVICE_HEARTBEAT",
              "schemaVersion": 1,
              "deviceId": "%s",
              "siteId": "%d",
              "deviceSequence": 6,
              "occurredAt": "2026-09-08T18:00:00Z",
              "payload": {}
            }
          ]
        }
        """
            .formatted(firstEventId, device.deviceId(), hqId);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events", device.accessToken(), first))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"));

    String conflict =
        """
        {
          "events": [
            {
              "eventId": "%s",
              "eventType": "DEVICE_HEARTBEAT",
              "schemaVersion": 1,
              "deviceId": "%s",
              "siteId": "%d",
              "deviceSequence": 6,
              "occurredAt": "2026-09-08T18:01:00Z",
              "payload": {}
            }
          ]
        }
        """
            .formatted(UUID.randomUUID(), device.deviceId(), hqId);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events", device.accessToken(), conflict))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results[0].status").value("REJECTED"))
        .andExpect(jsonPath("$.results[0].message").value(org.hamcrest.Matchers.containsString("deviceSequence")));
  }

  private Inventory stockAtPos(long hqId, long itemId) {
    var loc = posLocationUseCases.findPosLocation(hqId).orElseThrow();
    return inventoryRepository
        .findByItemIdAndLocationId(itemId, loc.getId())
        .orElseThrow();
  }

  private record EnrolledDevice(UUID deviceId, String accessToken) {}

  private EnrolledDevice enrollDevice(String staffToken, long hqId, String deviceName)
      throws Exception {
    UUID devicePublicId = UUID.randomUUID();
    String code = createEnrollmentCode(staffToken, hqId);
    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll",
                    enrollJson(code, devicePublicId, deviceName)))
            .andExpect(status().isOk())
            .andReturn();
    String access = JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");
    String deviceId = JsonPath.read(enroll.getResponse().getContentAsString(), "$.deviceId");
    return new EnrolledDevice(UUID.fromString(deviceId), access);
  }

  private static String shiftLifecycleEventJson(
      EnrolledDevice device,
      long siteId,
      UUID shiftId,
      UUID eventId,
      long deviceSequence,
      String eventType,
      UUID aggregateId,
      String payloadJson) {
    return """
        {
          "events": [
            {
              "eventId": "%s",
              "eventType": "%s",
              "schemaVersion": 1,
              "deviceId": "%s",
              "siteId": "%d",
              "deviceSequence": %d,
              "aggregateId": "%s",
              "shiftId": "%s",
              "occurredAt": "2026-09-08T18:00:00Z",
              "payload": %s
            }
          ]
        }
        """
        .formatted(
            eventId,
            eventType,
            device.deviceId(),
            siteId,
            deviceSequence,
            aggregateId,
            shiftId,
            payloadJson);
  }

  private String saleEventJsonForShift(
      EnrolledDevice device,
      long siteId,
      UUID eventId,
      UUID saleId,
      UUID shiftId,
      long productId,
      long operatorId,
      int quantity,
      long unitPriceCentavos,
      boolean soldWithNegativeStock,
      String paymentMethod) {
    UUID lineId = UUID.randomUUID();
    UUID paymentId = UUID.randomUUID();
    long subtotal = unitPriceCentavos * quantity;
    return """
        {
          "events": [
            {
              "eventId": "%s",
              "eventType": "SALE_CONFIRMED",
              "schemaVersion": 1,
              "deviceId": "%s",
              "siteId": "%d",
              "deviceSequence": 100,
              "aggregateId": "%s",
              "shiftId": "%s",
              "occurredAt": "2026-09-08T15:23:12Z",
              "payload": {
                "saleId": "%s",
                "folio": "T1-104-0001",
                "cashierOperatorId": "%d",
                "grossCentavos": %d,
                "discountCentavos": 0,
                "totalCentavos": %d,
                "status": "CONFIRMED",
                "lines": [
                  {
                    "lineId": "%s",
                    "productId": "%d",
                    "productName": "Producto",
                    "saleCategory": "Bebidas",
                    "quantity": %d,
                    "unit": "PIECE",
                    "unitPriceCentavos": %d,
                    "subtotalCentavos": %d,
                    "stockPolicy": "CONTROLLED",
                    "soldWithNegativeStock": %s,
                    "soldWhileUnavailable": false,
                    "rawBarcode": null
                  }
                ],
                "payments": [
                  {
                    "paymentId": "%s",
                    "method": "%s",
                    "amountCentavos": %d,
                    "tenderedCentavos": %d,
                    "changeCentavos": 0
                  }
                ],
                "discount": null
              }
            }
          ]
        }
        """
        .formatted(
            eventId,
            device.deviceId(),
            siteId,
            saleId,
            shiftId,
            saleId,
            operatorId,
            subtotal,
            subtotal,
            lineId,
            productId,
            quantity,
            unitPriceCentavos,
            subtotal,
            soldWithNegativeStock,
            paymentId,
            paymentMethod,
            subtotal,
            subtotal);
  }

  private String saleEventJson(
      EnrolledDevice device,
      long siteId,
      UUID eventId,
      UUID saleId,
      long productId,
      long operatorId,
      int quantity,
      long unitPriceCentavos,
      boolean soldWithNegativeStock) {
    return saleEventJsonForShift(
        device,
        siteId,
        eventId,
        saleId,
        UUID.randomUUID(),
        productId,
        operatorId,
        quantity,
        unitPriceCentavos,
        soldWithNegativeStock,
        "CASH");
  }

  private long createOperator(String staffToken, long hqId, String name) throws Exception {
    return createOperator(staffToken, hqId, name, "CASHIER");
  }

  private long createOperator(String staffToken, long hqId, String name, String role) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/operators",
                    staffToken,
                    """
                    {
                      "displayName": "%s",
                      "posRole": "%s",
                      "pin": "1234",
                      "headquarterIds": [%d]
                    }
                    """
                        .formatted(name, role, hqId)))
            .andExpect(status().isOk())
            .andReturn();
    Number n = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    return n.longValue();
  }

  private String createEnrollmentCode(String staffToken, long hqId) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/enrollment-codes",
                    staffToken,
                    "{\"headquarterId\": %d}".formatted(hqId)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(r.getResponse().getContentAsString(), "$.code");
  }

  private void putPosSettings(String token, long hqId) throws Exception {
    putPosSettings(token, hqId, false);
  }

  private void putPosSettings(String token, long hqId, boolean allowOpenProducts) throws Exception {
    String body =
        """
        {
          "currency": "MXN",
          "catalogStaleWarnHours": 24,
          "catalogStaleBlockHours": 72,
          "openAmountCategories": ["MISC"],
          "allowOpenProducts": %s,
          "defaultNegativeStockLimit": 10
        }
        """.formatted(allowOpenProducts);
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-settings", token, body))
        .andExpect(status().isOk());
  }

  private String openSaleEventJson(
      EnrolledDevice device, long siteId, UUID eventId, UUID saleId, long seedProductId, long authorizerId)
      throws Exception {
    String body = saleEventJson(device, siteId, eventId, saleId, seedProductId, authorizerId, 1, 4000, false);
    return body
        .replace("\"productId\": \"" + seedProductId + "\"", "\"lineType\": \"OPEN_AMOUNT\",\n                     \"productId\": null")
        .replace("\"productName\": \"Producto\"", "\"productName\": \"Producto abierto · MISC\"")
        .replace("\"saleCategory\": \"Bebidas\"", "\"saleCategory\": \"MISC\"")
        .replace("\"stockPolicy\": \"CONTROLLED\"", "\"stockPolicy\": \"NOT_CONTROLLED\"")
        .replace("\"rawBarcode\": null", "\"rawBarcode\": null,\n                     \"authorizedByOperatorId\": " + authorizerId + ",\n                     \"authorizedAt\": \"2026-09-14T17:00:00Z\"");
  }

  private void putCatalog(
      String token, long hqId, long itemId, String saleCategory, String price, String stockPolicy)
      throws Exception {
    String body =
        """
        {
          "saleCategory": "%s",
          "salePrice": %s,
          "available": true,
          "stockPolicy": "%s",
          "negativeStockLimit": 5
        }
        """
            .formatted(saleCategory, price, stockPolicy);
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-catalog/" + itemId, token, body))
        .andExpect(status().isOk());
  }

  private long createItem(String token, String sku, String name) throws Exception {
    String body =
        """
        {
          "sku": "%s",
          "name": "%s",
          "description": "IT",
          "costPrice": 10.00,
          "category": "CONSUMABLE",
          "unit": "PIECE",
          "reorderPoint": 0,
          "reorderQuantity": 0
        }
        """
            .formatted(sku, name);
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/inventory/items", body)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    Number n = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    return n.longValue();
  }

  private static String enrollJson(String code, UUID deviceId, String name) {
    return """
        {
          "enrollmentCode": "%s",
          "devicePublicId": "%s",
          "deviceName": "%s",
          "appVersion": "1.0.0"
        }
        """
        .formatted(code, deviceId, name);
  }

  private long createHeadquarter(String token, String name) throws Exception {
    String body =
        """
        {"name": "%s", "address": "Addr", "description": "d"}
        """
            .formatted(name);
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/headquarters", body)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    Number n = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-pos-b4-" + UUID.randomUUID() + "@mail.com";
    String phone =
        "+52"
            + String.format(
                "%010d", Math.abs(ThreadLocalRandom.current().nextLong()) % 10_000_000_000L);
    String password = "Str0ngPass!";
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(AccountTestRequests.validRegisterJson(email, phone, password)))
        .andExpect(status().isCreated());

    UserJpaEntity u =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new AssertionError("user missing"));
    u.setAccountStatus(AccountStatus.ACTIVE);
    u.setRoles(new LinkedHashSet<>(Set.of(Role.ADMIN)));
    userJpaRepository.saveAndFlush(u);

    MvcResult login =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
  }
}
