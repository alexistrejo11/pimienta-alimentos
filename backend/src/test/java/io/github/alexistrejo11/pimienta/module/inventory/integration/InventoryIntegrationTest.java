package io.github.alexistrejo11.pimienta.module.inventory.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class InventoryIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void items_search_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/inventory/items")).andExpect(status().isUnauthorized());
  }

  @Test
  void items_create_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(AccountTestRequests.postJson("/api/v1/inventory/items", minimalItemCreateJson("X", "N")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void items_create_blankSku_generatesInternalSku() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/items", minimalItemCreateJson("   ", "Name"))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sku").value(org.hamcrest.Matchers.startsWith("CAF-")));
  }

  @Test
  void items_create_malformedJson_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/inventory/items")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void items_getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/items/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("ITEM_NOT_FOUND"));
  }

  @Test
  void items_getById_invalidPath_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/items/not-id", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
  }

  @Test
  void items_lookup_missingQueryParam_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/items/lookup", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"));
  }

  @Test
  void items_duplicateSku_returns409() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String sku = "IT-DUP-" + suffix;
    String body = minimalItemCreateJson(sku, "First " + suffix);

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/items", body)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/items", body)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("ITEM_SKU_ALREADY_EXISTS"));
  }

  @Test
  void locations_getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/locations/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("STORAGE_LOCATION_NOT_FOUND"));
  }

  @Test
  void stock_getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/stock/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("INVENTORY_NOT_FOUND"));
  }

  @Test
  void movements_getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/movements/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("INVENTORY_MOVEMENT_NOT_FOUND"));
  }

  @Test
  void transactions_getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/transactions/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("INVENTORY_TRANSACTION_NOT_FOUND"));
  }

  @Test
  void transactions_purchase_emptyLines_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/transactions/purchase", "{\"lines\":[]}")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void stock_createInitialStock_duplicate_returns409() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    long locId = createWarehouseLocation(token, "WH-DUP-" + suffix, "Dup WH " + suffix);
    long itemId = createItem(token, "SKU-DUP-" + suffix, "Item dup " + suffix);

    String stockBody = "{\"itemId\": %d, \"locationId\": %d, \"initialQuantity\": 1}".formatted(itemId, locId);
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/stock", stockBody)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/stock", stockBody)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("INVENTORY_ALREADY_EXISTS"));
  }

  @Test
  void storageLocation_delete_whenHasInventory_returns409() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    long locId = createWarehouseLocation(token, "WH-NODEL-" + suffix, "No del " + suffix);
    long itemId = createItem(token, "SKU-NODEL-" + suffix, "Item " + suffix);
    String stockBody = "{\"itemId\": %d, \"locationId\": %d, \"initialQuantity\": 5}".formatted(itemId, locId);
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/stock", stockBody)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated());

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/inventory/locations/" + locId, token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("STORAGE_LOCATION_NOT_EMPTY"));
  }

  @Test
  void stock_createInBlockedLocation_returns400ValidationFailed() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    long locId = createWarehouseLocation(token, "WH-BLK-" + suffix, "Blocked " + suffix);
    mockMvc
        .perform(
            put("/api/v1/inventory/locations/" + locId + "/block")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    long itemId = createItem(token, "SKU-BLK-" + suffix, "Item blk " + suffix);
    String stockBody = "{\"itemId\": %d, \"locationId\": %d, \"initialQuantity\": 1}".formatted(itemId, locId);
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/stock", stockBody)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void inventory_endToEnd_itemLocationStockPurchaseSaleMovements_flow() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String whCode = "WH-IT-" + suffix;
    String locBody =
        """
            {"code": "%s", "name": "Warehouse %s", "description": "IT", "type": "WAREHOUSE", "maxCapacity": 10000}
            """
            .formatted(whCode, suffix);

    MvcResult locRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/inventory/locations", locBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value(whCode))
            .andReturn();
    long locationId = extractLongId(locRes.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/locations/tree", token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/inventory/locations/parent/" + locationId + "/children", token))
        .andExpect(status().isOk());

    String zoneBody =
        """
            {
              "code": "ZN-%s",
              "name": "Cold zone",
              "description": "",
              "type": "ZONE",
              "parentId": %d,
              "maxCapacity": 5000
            }
            """
            .formatted(suffix, locationId);
    MvcResult zoneRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/inventory/locations", zoneBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.parentId").value(locationId))
            .andReturn();
    long zoneId = extractLongId(zoneRes.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/inventory/locations/" + zoneId,
                token,
                """
                    {
                      "code": "ZN-%s",
                      "name": "Cold zone updated",
                      "description": "",
                      "type": "ZONE",
                      "parentId": %d,
                      "maxCapacity": 5000,
                      "occupiedCapacity": 0,
                      "status": "ACTIVE"
                    }
                    """
                    .formatted(suffix, locationId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Cold zone updated"));

    String sku = "SKU-FLOW-" + suffix;
    String itemBody = minimalItemCreateJson(sku, "Flow item " + suffix);
    MvcResult itemRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/inventory/items", itemBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value(sku))
            .andReturn();
    long itemId = extractLongId(itemRes.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/items/lookup?q=" + sku, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(itemId));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/items?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata").exists());

    mockMvc
        .perform(
            put("/api/v1/inventory/items/" + itemId + "/discontinue")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISCONTINUED"));

    mockMvc
        .perform(
            put("/api/v1/inventory/items/" + itemId + "/activate")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    String updateItem =
        """
            {
              "sku": "%s",
              "name": "Flow item updated",
              "description": "d",
              "costPrice": 11.50,
              "salePrice": 19.99,
              "category": "RAW_MATERIAL",
              "unit": "PIECE",
              "reorderPoint": 2,
              "reorderQuantity": 10,
              "status": "ACTIVE"
            }
            """
            .formatted(sku);
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer("/api/v1/inventory/items/" + itemId, token, updateItem))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Flow item updated"));

    String stockBody =
        "{\"itemId\": %d, \"locationId\": %d, \"initialQuantity\": 100}".formatted(itemId, locationId);
    MvcResult stockRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/inventory/stock", stockBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.availableQuantity").value(100))
            .andReturn();
    long inventoryId = extractLongId(stockRes.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/stock/" + inventoryId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(inventoryId));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/stock/item/" + itemId, token))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/stock/location/" + locationId, token))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/stock/low-stock?page=0&size=10", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/stock/out-of-stock?page=0&size=10", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    String extRef = "PO-IT-" + suffix;
    String purchaseBody =
        """
            {
              "externalReference": "%s",
              "notes": "IT purchase",
              "lines": [{"itemId": %d, "locationId": %d, "quantity": 20, "unitCost": 5.25}]
            }
            """
            .formatted(extRef, itemId, locationId);

    MvcResult purchaseRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/inventory/transactions/purchase", purchaseBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.type").value("PURCHASE_RECEIPT"))
            .andReturn();
    long purchaseTxId = extractLongId(purchaseRes.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/transactions/" + purchaseTxId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(purchaseTxId));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/inventory/movements/by-reference?referenceNumber=" + extRef, token))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/movements/item/" + itemId, token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/inventory/movements/location/" + locationId, token))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/movements?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    String saleBody =
        """
            {
              "externalReference": "SO-%s",
              "lines": [{"itemId": %d, "locationId": %d, "quantity": 5, "unitCost": 5.00}]
            }
            """
            .formatted(suffix, itemId, locationId);
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/inventory/transactions/sale", saleBody)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc
        .perform(
            post("/api/v1/inventory/transactions/" + purchaseTxId + "/cancel")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/inventory/items/" + itemId, token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/inventory/items/" + itemId, token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("ITEM_NOT_FOUND"));
  }

  private long createWarehouseLocation(String token, String code, String name) throws Exception {
    String body =
        """
            {"code": "%s", "name": "%s", "description": "", "type": "WAREHOUSE", "maxCapacity": 1000}
            """
            .formatted(code, name);
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/inventory/locations", body)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    return extractLongId(r.getResponse().getContentAsString(), "$.id");
  }

  private long createItem(String token, String sku, String name) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/inventory/items", minimalItemCreateJson(sku, name))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    return extractLongId(r.getResponse().getContentAsString(), "$.id");
  }

  private static String minimalItemCreateJson(String sku, String name) {
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
        }
        """
        .formatted(sku.replace("\\", "\\\\").replace("\"", "\\\""), name.replace("\\", "\\\\").replace("\"", "\\\""));
  }

  private static long extractLongId(String json, String path) {
    Number n = JsonPath.read(json, path);
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-inv-" + UUID.randomUUID() + "@mail.com";
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
