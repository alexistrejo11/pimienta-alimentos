package io.github.alexistrejo11.pimienta.module.pos.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
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
class PosAdminSyncIncidentIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void syncIncidents_acceptAdminOnly_doesNotMutateSale_andUnlocksReports() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    TokenPair manager = obtainToken(Set.of(Role.MANAGER));

    long hqId = createHeadquarter(admin.token(), "POS-B6-HQ-" + UUID.randomUUID());
    assignHeadquarters(admin.token(), manager.userId(), hqId);
    putPosSettings(admin.token(), hqId);
    long itemId = createItem(admin.token(), "SKU-B6-" + UUID.randomUUID(), "Refresco B6");
    putCatalog(admin.token(), hqId, itemId, "Bebidas", "25.00", "CONTROLLED");
    long operatorId = createOperator(admin.token(), hqId, "Cajera B6");
    EnrolledDevice device = enrollDevice(admin.token(), hqId, "Caja B6");

    UUID reviewEventId = UUID.randomUUID();
    UUID reviewSaleId = UUID.randomUUID();
    String reviewBody =
        saleEventJson(device, hqId, reviewEventId, reviewSaleId, itemId, operatorId, 2, 2500, true);

    MvcResult reviewIngest =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/sync/events", device.accessToken(), reviewBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results[0].status").value("REQUIRES_REVIEW"))
            .andExpect(jsonPath("$.results[0].incidentId").isNotEmpty())
            .andReturn();
    String incidentId =
        JsonPath.read(reviewIngest.getResponse().getContentAsString(), "$.results[0].incidentId");

    mockMvc
        .perform(get("/api/v1/pos/admin/sync-incidents/" + incidentId))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/pos/admin/sync-incidents/" + incidentId, manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(incidentId))
        .andExpect(jsonPath("$.acceptedAt", nullValue()));

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/admin/sync-incidents/" + incidentId + "/accept",
                manager.token(),
                """
                {"label":"OK","note":"manager cannot accept"}
                """))
        .andExpect(status().isForbidden());

    String reportsUrl =
        "/api/v1/pos/admin/reports/sales?headquarterId=%d&from=2026-09-01T00:00:00Z&to=2026-09-30T00:00:00Z"
            .formatted(hqId);
    mockMvc
        .perform(AccountTestRequests.getBearer(reportsUrl, admin.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].saleId").value(reviewSaleId.toString()))
        .andExpect(jsonPath("$.items[0].syncStatus").value("REQUIRES_REVIEW"));

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/admin/sync-incidents/" + incidentId + "/accept",
                admin.token(),
                """
                {"label":"REVIEWED","note":"Neg stock acknowledged"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.acceptLabel").value("REVIEWED"))
        .andExpect(jsonPath("$.acceptNote").value("Neg stock acknowledged"))
        .andExpect(jsonPath("$.acceptedBy").value(admin.userId().intValue()))
        .andExpect(jsonPath("$.acceptedAt").isNotEmpty());

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/admin/sync-incidents/" + incidentId + "/accept",
                admin.token(),
                """
                {"label":"AGAIN","note":"should conflict"}
                """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("POS_SYNC_INCIDENT_ALREADY_ACCEPTED"));

    mockMvc
        .perform(AccountTestRequests.getBearer(reportsUrl, admin.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].saleId").value(reviewSaleId.toString()))
        .andExpect(jsonPath("$.items[0].totalCentavos").value(5000))
        .andExpect(jsonPath("$.items[0].syncStatus").value("ACCEPTED"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/pos/admin/reports/products?headquarterId=%d&from=2026-09-01T00:00:00Z&to=2026-09-30T00:00:00Z"
                    .formatted(hqId),
                admin.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].quantitySum").value(2))
        .andExpect(jsonPath("$.items[0].subtotalCentavosSum").value(5000));
  }

  @Test
  void reports_wasteAndShiftClose_smokeFromLedger() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hqId = createHeadquarter(admin.token(), "POS-B6-LED-" + UUID.randomUUID());
    putPosSettings(admin.token(), hqId);
    EnrolledDevice device = enrollDevice(admin.token(), hqId, "Caja Led");

    UUID wasteEventId = UUID.randomUUID();
    UUID shiftEventId = UUID.randomUUID();
    UUID shiftId = UUID.randomUUID();
    String batch =
        """
        {
          "events": [
            {
              "eventId": "%s",
              "eventType": "WASTE_RECORDED",
              "schemaVersion": 1,
              "deviceId": "%s",
              "siteId": "%d",
              "deviceSequence": 1,
              "aggregateId": null,
              "shiftId": "%s",
              "occurredAt": "2026-09-08T16:00:00Z",
              "payload": { "reason": "spillage", "quantity": 1 }
            },
            {
              "eventId": "%s",
              "eventType": "SHIFT_CLOSED",
              "schemaVersion": 1,
              "deviceId": "%s",
              "siteId": "%d",
              "deviceSequence": 2,
              "aggregateId": "%s",
              "shiftId": "%s",
              "occurredAt": "2026-09-08T17:00:00Z",
              "payload": { "cashExpectedCentavos": 10000 }
            }
          ]
        }
        """
            .formatted(
                wasteEventId,
                device.deviceId(),
                hqId,
                shiftId,
                shiftEventId,
                device.deviceId(),
                hqId,
                shiftId,
                shiftId);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/events", device.accessToken(), batch))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.results", hasSize(2)))
        .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"))
        // A close without a previously materialized shift is rejected by the
        // authoritative lifecycle projection.
        .andExpect(jsonPath("$.results[1].status").value("REJECTED"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/pos/admin/reports/waste-cancellations?headquarterId=%d&from=2026-09-01T00:00:00Z&to=2026-09-30T00:00:00Z"
                    .formatted(hqId),
                admin.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].eventId").value(wasteEventId.toString()))
        .andExpect(jsonPath("$.items[0].eventType").value("WASTE_RECORDED"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/pos/admin/reports/shift-closes?headquarterId=%d&from=2026-09-01T00:00:00Z&to=2026-09-30T00:00:00Z"
                    .formatted(hqId),
                admin.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)));
  }

  private record EnrolledDevice(UUID deviceId, String accessToken) {}

  private record TokenPair(String token, Long userId) {}

  private TokenPair obtainToken(Set<Role> roles) throws Exception {
    String email = "it-pos-b6-" + UUID.randomUUID() + "@mail.com";
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
    u.setRoles(new LinkedHashSet<>(roles));
    userJpaRepository.saveAndFlush(u);

    MvcResult login =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    return new TokenPair(token, u.getId());
  }

  private EnrolledDevice enrollDevice(String staffToken, long hqId, String deviceName)
      throws Exception {
    UUID devicePublicId = UUID.randomUUID();
    String code = createEnrollmentCode(staffToken, hqId);
    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll", enrollJson(code, devicePublicId, deviceName)))
            .andExpect(status().isOk())
            .andReturn();
    String access = JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");
    String deviceId = JsonPath.read(enroll.getResponse().getContentAsString(), "$.deviceId");
    return new EnrolledDevice(UUID.fromString(deviceId), access);
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
    UUID lineId = UUID.randomUUID();
    UUID paymentId = UUID.randomUUID();
    UUID shiftId = UUID.randomUUID();
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
                "folio": "T1-B6-0001",
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
                    "method": "CASH",
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
            subtotal,
            subtotal);
  }

  private long createOperator(String staffToken, long hqId, String name) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/operators",
                    staffToken,
                    """
                    {
                      "displayName": "%s",
                      "posRole": "CASHIER",
                      "pin": "1234",
                      "headquarterIds": [%d]
                    }
                    """
                        .formatted(name, hqId)))
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
    String body =
        """
        {
          "currency": "MXN",
          "catalogStaleWarnHours": 24,
          "catalogStaleBlockHours": 72,
          "openAmountCategories": ["MISC"],
          "defaultNegativeStockLimit": 10
        }
        """;
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-settings", token, body))
        .andExpect(status().isOk());
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

  private void assignHeadquarters(String adminToken, long userId, long headquarterId)
      throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/users/management/" + userId + "/headquarters",
                adminToken,
                """
                {"headquarterIds":[%d]}
                """
                    .formatted(headquarterId)))
        .andExpect(status().isOk());
  }
}
