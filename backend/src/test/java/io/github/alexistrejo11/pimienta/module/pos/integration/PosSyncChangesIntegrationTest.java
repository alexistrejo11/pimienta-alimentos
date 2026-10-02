package io.github.alexistrejo11.pimienta.module.pos.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.HeadquarterItem;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.output.HeadquarterItemRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosChangeLogJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosChangeLogSpringDataRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
class PosSyncChangesIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private HeadquarterItemRepository headquarterItemRepository;
  @Autowired private PosChangeLogSpringDataRepository posChangeLogRepository;

  @Test
  void changes_afterBootstrap_emptyThenUpsertOnCatalogMutation() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B5-A-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-B5-" + UUID.randomUUID(), "Producto B5");
    putCatalog(staffToken, hqId, itemId, "Bebidas", "25.00");

    String access = enrollDevice(staffToken, hqId, "Caja B5");
    String cursor = bootstrapCursor(access);

    mockMvc
        .perform(AccountTestRequests.getBearer(changesUrl(cursor), access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.schemaVersion").value(1))
        .andExpect(jsonPath("$.operations", hasSize(0)))
        .andExpect(jsonPath("$.nextCursor").value(matchesPattern("cursor-hq-" + hqId + "-s\\d+")));

    Thread.sleep(15);
    putCatalog(staffToken, hqId, itemId, "Bebidas", "28.00");

    mockMvc
        .perform(AccountTestRequests.getBearer(changesUrl(cursor), access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operations[?(@.entity=='product' && @.op=='upsert')]", hasSize(1)))
        .andExpect(jsonPath("$.operations[?(@.entity=='product')].id").value(String.valueOf(itemId)))
        .andExpect(
            jsonPath("$.operations[?(@.entity=='product')].data.priceCentavos").value(2800))
        .andExpect(jsonPath("$.nextCursor").value(matchesPattern("cursor-hq-" + hqId + "-s\\d+")));
  }

  @Test
  void changes_productNameOrBarcode_emitsUpsertForTablets() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B5-NAME-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    String barcode = "750" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
    long productId = createItem(staffToken, barcode, "CUERNITOS");
    putCatalog(staffToken, hqId, productId, "Pan", "12.00");

    String access = enrollDevice(staffToken, hqId, "Caja nombre");
    String cursor = bootstrapCursor(access);

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/pos/sync/products/" + productId,
                access,
                "{\"name\": \"Cuernito\", \"barcode\": \"%s\"}".formatted(barcode)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Cuernito"));

    MvcResult renamed =
        mockMvc
            .perform(AccountTestRequests.getBearer(changesUrl(cursor), access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operations[?(@.entity=='product' && @.op=='upsert')]", hasSize(1)))
            .andExpect(jsonPath("$.operations[?(@.entity=='product')].data.name").value("Cuernito"))
            .andExpect(jsonPath("$.operations[?(@.entity=='product')].data.barcode").value(barcode))
            .andReturn();
    String afterRename =
        JsonPath.read(renamed.getResponse().getContentAsString(), "$.nextCursor");

    String replacement = "750" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);
    updateProduct(staffToken, productId, "Cuernito", replacement, "");

    MvcResult recoded =
        mockMvc
            .perform(AccountTestRequests.getBearer(changesUrl(afterRename), access))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operations[?(@.entity=='product' && @.op=='upsert')]", hasSize(1)))
            .andExpect(jsonPath("$.operations[?(@.entity=='product')].data.name").value("Cuernito"))
            .andExpect(jsonPath("$.operations[?(@.entity=='product')].data.barcode").value(replacement))
            .andReturn();
    String afterBarcode =
        JsonPath.read(recoded.getResponse().getContentAsString(), "$.nextCursor");

    updateProduct(staffToken, productId, "Cuernito", replacement, "solo descripcion");

    mockMvc
        .perform(AccountTestRequests.getBearer(changesUrl(afterBarcode), access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.operations", hasSize(0)));
  }

  @Test
  void changes_softDeletedProduct_emitsDeactivate() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B5-DEL-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-B5-DEL-" + UUID.randomUUID(), "To delete");
    putCatalog(staffToken, hqId, itemId, "Otros", "10.00");

    String access = enrollDevice(staffToken, hqId, "Caja Del");
    String cursor = bootstrapCursor(access);
    Thread.sleep(15);

    HeadquarterItem row =
        headquarterItemRepository
            .findByHeadquarterIdAndProductId(hqId, itemId)
            .orElseThrow(() -> new AssertionError("catalog row missing"));
    row.softDelete();
    headquarterItemRepository.save(row);

    mockMvc
        .perform(AccountTestRequests.getBearer(changesUrl(cursor), access))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.operations[?(@.entity=='product' && @.op=='deactivate' && @.id=='%s')]"
                        .formatted(itemId),
                    hasSize(1)));
  }

  @Test
  void changes_unassignOperator_emitsDeactivate() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B5-OP-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/operators",
                    staffToken,
                    """
                    {
                      "displayName": "Cajera B5",
                      "posRole": "CASHIER",
                      "pin": "1234",
                      "headquarterIds": [%d]
                    }
                    """
                        .formatted(hqId)))
            .andExpect(status().isOk())
            .andReturn();
    Number opIdNum = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
    long opId = opIdNum.longValue();

    String access = enrollDevice(staffToken, hqId, "Caja Op");
    String cursor = bootstrapCursor(access);
    Thread.sleep(15);

    mockMvc
        .perform(
            delete("/api/v1/pos/admin/operators/" + opId + "/headquarters/" + hqId)
                .header("Authorization", "Bearer " + staffToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer(changesUrl(cursor), access))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$.operations[?(@.entity=='operator' && @.op=='deactivate' && @.id=='%s')]"
                        .formatted(opId),
                    hasSize(1)));
  }

  @Test
  void changes_invalidOrForeignCursor_returns409() throws Exception {
    String staffToken = obtainAccessToken();
    long hqA = createHeadquarter(staffToken, "POS-B5-CUR-A-" + UUID.randomUUID());
    long hqB = createHeadquarter(staffToken, "POS-B5-CUR-B-" + UUID.randomUUID());
    putPosSettings(staffToken, hqA);
    putPosSettings(staffToken, hqB);

    String access = enrollDevice(staffToken, hqA, "Caja Cur");

    mockMvc
        .perform(AccountTestRequests.getBearer(changesUrl("not-a-cursor"), access))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("POS_SYNC_CURSOR_INVALID"));

    mockMvc
        .perform(AccountTestRequests.getBearer(changesUrl("cursor-hq-" + hqB + "-s1"), access))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("POS_SYNC_CURSOR_INVALID"));
  }

  @Test
  void changes_revokedDevice_returns403() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B5-REV-" + UUID.randomUUID());
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
    String cursor = bootstrapCursor(access);

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/pos/admin/devices/" + deviceId + "/revoke", staffToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer(changesUrl(cursor), access))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("POS_DEVICE_REVOKED"));
  }

  @Test
  void changes_cursorOlderThanRetainedHistory_returns409() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B5-RET-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-B5-RET-" + UUID.randomUUID(), "Retention item");
    putCatalog(staffToken, hqId, itemId, "Otros", "10.00");
    putCatalog(staffToken, hqId, itemId, "Otros", "11.00");
    String access = enrollDevice(staffToken, hqId, "Caja Ret");

    PosChangeLogJpaEntity firstRetained =
        posChangeLogRepository
            .findFirstByHeadquarterIdOrderBySequenceAsc(hqId)
            .orElseThrow(() -> new AssertionError("change log is empty"));
    long oldCursorSequence = Math.max(0L, firstRetained.getSequence() - 2L);
    posChangeLogRepository.deleteById(firstRetained.getSequence());
    posChangeLogRepository.flush();

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                changesUrl("cursor-hq-" + hqId + "-s" + oldCursorSequence), access))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("POS_SYNC_CURSOR_INVALID"));
  }

  private static String changesUrl(String cursor) {
    return "/api/v1/pos/sync/changes?cursor="
        + URLEncoder.encode(cursor, StandardCharsets.UTF_8);
  }

  private String bootstrapCursor(String access) throws Exception {
    MvcResult bootstrap =
        mockMvc
            .perform(AccountTestRequests.getBearer("/api/v1/pos/sync/bootstrap", access))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(bootstrap.getResponse().getContentAsString(), "$.cursors.changes");
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

  private void updateProduct(
      String token, long productId, String name, String barcode, String description) throws Exception {
    String body =
        """
        {
          "name": "%s",
          "description": "%s",
          "unit": "PIECE",
          "barcode": "%s",
          "status": "ACTIVE",
          "trackStock": true
        }
        """
            .formatted(name, description, barcode);
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer("/api/v1/products/" + productId, token, body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(name))
        .andExpect(jsonPath("$.barcode").value(barcode));
  }

  private long createItem(String token, String sku, String name) throws Exception {
    String body =
        """
        {
          "name": "%s",
          "unit": "PIECE",
          "barcode": "%s",
          "trackStock": true
        }
        """
            .formatted(name, sku);
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/products", body)
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
    String email = "it-pos-b5-" + UUID.randomUUID() + "@mail.com";
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
