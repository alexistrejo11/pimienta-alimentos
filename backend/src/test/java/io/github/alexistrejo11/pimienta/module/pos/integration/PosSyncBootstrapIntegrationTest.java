package io.github.alexistrejo11.pimienta.module.pos.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
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
class PosSyncBootstrapIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void bootstrap_returnsCatalogAndOperatorsForDeviceSiteOnly() throws Exception {
    String staffToken = obtainAccessToken();
    long hqA = createHeadquarter(staffToken, "POS-B3-A-" + UUID.randomUUID());
    long hqB = createHeadquarter(staffToken, "POS-B3-B-" + UUID.randomUUID());

    putPosSettings(staffToken, hqA);
    putPosSettings(staffToken, hqB);

    long itemA = createItem(staffToken, "SKU-A-" + UUID.randomUUID(), "Cafe A");
    long itemB = createItem(staffToken, "SKU-B-" + UUID.randomUUID(), "Cafe B");
    putCatalog(staffToken, hqA, itemA, "Deli", "40.00");
    putCatalog(staffToken, hqB, itemB, "Bebidas", "25.00");

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/admin/operators",
                staffToken,
                """
                {
                  "displayName": "Cajera A",
                  "posRole": "CASHIER",
                  "pin": "1234",
                  "headquarterIds": [%d]
                }
                """
                    .formatted(hqA)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/admin/operators",
                staffToken,
                """
                {
                  "displayName": "Cajera B",
                  "posRole": "CASHIER",
                  "pin": "5678",
                  "headquarterIds": [%d]
                }
                """
                    .formatted(hqB)))
        .andExpect(status().isOk());

    String access = enrollDevice(staffToken, hqA, "Caja A");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/sync/bootstrap", access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schemaVersion").value(1))
        .andExpect(jsonPath("$.kind").value("pos-bootstrap"))
        .andExpect(jsonPath("$.snapshotId", not(nullValue())))
        .andExpect(jsonPath("$.generatedAt", not(nullValue())))
        .andExpect(jsonPath("$.site.id").value(String.valueOf(hqA)))
        .andExpect(jsonPath("$.site.currency").value("MXN"))
        .andExpect(jsonPath("$.device.status").value("AUTHORIZED"))
        .andExpect(jsonPath("$.operators", hasSize(1)))
        .andExpect(jsonPath("$.operators[0].displayName").value("Cajera A"))
        .andExpect(jsonPath("$.operators[0].role").value("CASHIER"))
        .andExpect(jsonPath("$.operators[0].pinHash", not(nullValue())))
        .andExpect(jsonPath("$.operators[0].active").value(true))
        .andExpect(jsonPath("$.products", hasSize(1)))
        .andExpect(jsonPath("$.products[0].id").value(String.valueOf(itemA)))
        .andExpect(jsonPath("$.products[0].saleCategory").value("Deli"))
        .andExpect(jsonPath("$.products[0].priceCentavos").value(4000))
        .andExpect(jsonPath("$.products[0].costCentavos").value(1000))
        .andExpect(jsonPath("$.products[0].unit").value("PIECE"))
        .andExpect(jsonPath("$.products[0].stockPolicy").value("CONTROLLED"))
        .andExpect(jsonPath("$.openAmountCategories[0]").value("MISC"))
        .andExpect(jsonPath("$.policies.allowNegativeStock").value(true))
        .andExpect(jsonPath("$.policies.defaultNegativeStockLimit").value(10))
        .andExpect(jsonPath("$.policies.staleCatalogWarnHours").value(24))
        .andExpect(
            jsonPath("$.cursors.changes")
                .value(org.hamcrest.Matchers.matchesPattern("cursor-hq-" + hqA + "-v\\d+")));
  }

  @Test
  void bootstrap_otherHeadquarterCatalog_notVisible() throws Exception {
    String staffToken = obtainAccessToken();
    long hqA = createHeadquarter(staffToken, "POS-B3-ISO-A-" + UUID.randomUUID());
    long hqB = createHeadquarter(staffToken, "POS-B3-ISO-B-" + UUID.randomUUID());

    putPosSettings(staffToken, hqA);
    long itemB = createItem(staffToken, "SKU-ISO-B-" + UUID.randomUUID(), "Only B");
    putCatalog(staffToken, hqB, itemB, "Otros", "10.00");

    String access = enrollDevice(staffToken, hqA, "Iso A");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/sync/bootstrap", access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.site.id").value(String.valueOf(hqA)))
        .andExpect(jsonPath("$.products", hasSize(0)));
  }

  @Test
  void bootstrap_revokedDevice_returns403() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B3-REV-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);

    UUID deviceId = UUID.randomUUID();
    String code = createEnrollmentCode(staffToken, hqId);
    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll", enrollJson(code, deviceId, "Revoke")))
            .andExpect(status().isOk())
            .andReturn();
    String access = JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/pos/admin/devices/" + deviceId + "/revoke", staffToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/sync/bootstrap", access))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("POS_DEVICE_REVOKED"));
  }

  private String enrollDevice(String staffToken, long hqId, String deviceName) throws Exception {
    String code = createEnrollmentCode(staffToken, hqId);
    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll",
                    enrollJson(code, UUID.randomUUID(), deviceName)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");
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

  private void putCatalog(String token, long hqId, long itemId, String saleCategory, String price)
      throws Exception {
    String body =
        """
        {
          "saleCategory": "%s",
          "salePrice": %s,
          "available": true,
          "stockPolicy": "CONTROLLED",
          "negativeStockLimit": 5
        }
        """
            .formatted(saleCategory, price);
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
          "salePrice": 15.00,
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
    String email = "it-pos-b3-" + UUID.randomUUID() + "@mail.com";
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
