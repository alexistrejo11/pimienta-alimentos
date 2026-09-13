package io.github.alexistrejo11.pimienta.module.headquarter.integration;

import static org.hamcrest.Matchers.hasSize;
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
class HeadquarterPosCatalogIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void posSettings_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/headquarters/1/pos-settings"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void posCatalog_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/headquarters/1/pos-catalog"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void posSettings_upsertGet_ensuresPosLocation() throws Exception {
    String token = obtainAccessToken();
    long hqId = createHeadquarter(token, "POS-HQ-" + UUID.randomUUID());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/" + hqId + "/pos-settings", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("HEADQUARTER_POS_SETTINGS_NOT_FOUND"));

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
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headquarterId").value(hqId))
        .andExpect(jsonPath("$.currency").value("MXN"))
        .andExpect(jsonPath("$.catalogStaleWarnHours").value(24))
        .andExpect(jsonPath("$.openAmountCategories[0]").value("MISC"))
        .andExpect(jsonPath("$.defaultNegativeStockLimit").value(10));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/" + hqId + "/pos-settings", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currency").value("MXN"));

    String expectedCode = "POS-" + hqId;
    MvcResult locations =
        mockMvc
            .perform(
                AccountTestRequests.getBearer(
                    "/api/v1/inventory/locations?page=0&size=100&type=POS", token))
            .andExpect(status().isOk())
            .andReturn();
    String json = locations.getResponse().getContentAsString();
    org.junit.jupiter.api.Assertions.assertTrue(
        json.contains("\"code\":\"" + expectedCode + "\"")
            || json.contains("\"code\": \"" + expectedCode + "\""),
        "Expected POS location code " + expectedCode + " in: " + json);
    org.junit.jupiter.api.Assertions.assertTrue(
        json.contains("\"headquarterId\":" + hqId)
            || json.contains("\"headquarterId\": " + hqId),
        "Expected headquarterId " + hqId + " in: " + json);
  }

  @Test
  void posCatalog_putGetList_flow() throws Exception {
    String token = obtainAccessToken();
    long hqId = createHeadquarter(token, "CAT-HQ-" + UUID.randomUUID());
    long itemId = createItem(token, "SKU-POS-" + UUID.randomUUID(), "POS Item");

    String catalogBody =
        """
        {
          "saleCategory": "BEVERAGE",
          "salePrice": 25.50,
          "available": true,
          "stockPolicy": "CONTROLLED",
          "negativeStockLimit": 5
        }
        """;

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-catalog/" + itemId, token, catalogBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headquarterId").value(hqId))
        .andExpect(jsonPath("$.itemId").value(itemId))
        .andExpect(jsonPath("$.saleCategory").value("BEVERAGE"))
        .andExpect(jsonPath("$.salePrice").value(25.50))
        .andExpect(jsonPath("$.stockPolicy").value("CONTROLLED"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/headquarters/" + hqId + "/pos-catalog/" + itemId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.saleCategory").value("BEVERAGE"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/headquarters/" + hqId + "/pos-catalog?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].itemId").value(itemId));
  }

  @Test
  void posCatalog_candidates_returnsOnlySellableUnassignedItems() throws Exception {
    String token = obtainAccessToken();
    long hqId = createHeadquarter(token, "CANDIDATE-HQ-" + UUID.randomUUID());
    long candidateId = createItem(token, "SKU-CANDIDATE-" + UUID.randomUUID(), "Candidate", "POS_SELLABLE");
    createItem(token, "SKU-INVENTORY-" + UUID.randomUUID(), "Inventory only");

    mockMvc
        .perform(AccountTestRequests.getBearer(
            "/api/v1/headquarters/" + hqId + "/pos-catalog/candidates", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(candidateId))
        .andExpect(jsonPath("$[0].catalogRole").value("POS_SELLABLE"));

    String catalogBody = """
        {"saleCategory":"BEVERAGE","salePrice":25.50,"available":true,"stockPolicy":"CONTROLLED"}
        """;
    mockMvc.perform(AccountTestRequests.putJsonBearer(
        "/api/v1/headquarters/" + hqId + "/pos-catalog/" + candidateId, token, catalogBody))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer(
            "/api/v1/headquarters/" + hqId + "/pos-catalog/candidates", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  @Test
  void itemBarcode_duplicate_returns409() throws Exception {
    String token = obtainAccessToken();
    String barcode = "BC-" + UUID.randomUUID();

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/inventory/items", itemJson("SKU-A-" + UUID.randomUUID(), "A", barcode))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/inventory/items", itemJson("SKU-B-" + UUID.randomUUID(), "B", barcode))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("ITEM_BARCODE_ALREADY_EXISTS"));
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
    return extractLongId(r.getResponse().getContentAsString(), "$.id");
  }

  private long createItem(String token, String sku, String name) throws Exception {
    return createItem(token, sku, name, null);
  }

  private long createItem(String token, String sku, String name, String catalogRole) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/inventory/items", itemJson(sku, name, null, catalogRole))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    return extractLongId(r.getResponse().getContentAsString(), "$.id");
  }

  private static String itemJson(String sku, String name, String barcode) {
    return itemJson(sku, name, barcode, null);
  }

  private static String itemJson(String sku, String name, String barcode, String catalogRole) {
    String barcodeField =
        barcode == null ? "" : ", \"barcode\": \"%s\"".formatted(barcode.replace("\"", "\\\""));
    String roleField = catalogRole == null ? "" : ", \"catalogRole\": \"%s\"".formatted(catalogRole);
    return """
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
          %s%s
        }
        """
        .formatted(sku, name, barcodeField, roleField);
  }

  private static long extractLongId(String json, String path) {
    Number n = JsonPath.read(json, path);
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-hq-pos-" + UUID.randomUUID() + "@mail.com";
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
