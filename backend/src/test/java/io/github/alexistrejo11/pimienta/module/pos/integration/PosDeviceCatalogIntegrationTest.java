package io.github.alexistrejo11.pimienta.module.pos.integration;

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
class PosDeviceCatalogIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void createProduct_deviceJwt_createsSellableInternalSku() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-DEV-CAT-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    createSaleCategory(staffToken, hqId, "Bebidas");
    String access = enrollDevice(staffToken, hqId, "Caja catalogo");

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/products",
                access,
                """
                {
                  "name": "Agua natural",
                  "salePriceCentavos": 1500,
                  "saleCategory": "Bebidas"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", not(nullValue())))
        .andExpect(jsonPath("$.sku", not(nullValue())))
        .andExpect(jsonPath("$.barcode").doesNotExist())
        .andExpect(jsonPath("$.name").value("Agua natural"))
        .andExpect(jsonPath("$.saleCategory").value("Bebidas"))
        .andExpect(jsonPath("$.priceCentavos").value(1500))
        .andExpect(jsonPath("$.costCentavos").value(0))
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.stockPolicy").value("NOT_CONTROLLED"))
        .andExpect(jsonPath("$.unit").value("PIECE"));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/sync/bootstrap", access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products[0].name").value("Agua natural"))
        .andExpect(jsonPath("$.products[0].stockPolicy").value("NOT_CONTROLLED"));
  }

  @Test
  void createProduct_duplicateBarcode_returns409() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-DEV-BAR-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    createSaleCategory(staffToken, hqId, "Snacks");
    String access = enrollDevice(staffToken, hqId, "Caja barcode");
    String barcode = "750" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L);

    String body =
        """
        {
          "name": "Papas",
          "salePriceCentavos": 2000,
          "saleCategory": "Snacks",
          "barcode": "%s"
        }
        """
            .formatted(barcode);
    mockMvc
        .perform(AccountTestRequests.postJsonBearer("/api/v1/pos/sync/products", access, body))
        .andExpect(status().isCreated());
    mockMvc
        .perform(AccountTestRequests.postJsonBearer("/api/v1/pos/sync/products", access, body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("ITEM_BARCODE_ALREADY_EXISTS"));
  }

  @Test
  void createProduct_staffJwt_returns403() throws Exception {
    String staffToken = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/products",
                staffToken,
                """
                {
                  "name": "X",
                  "salePriceCentavos": 100,
                  "saleCategory": "Bebidas"
                }
                """))
        .andExpect(status().isForbidden());
  }

  @Test
  void createProduct_unknownCategory_returns404() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-DEV-CAT-MISS-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    String access = enrollDevice(staffToken, hqId, "Caja miss");
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/sync/products",
                access,
                """
                {
                  "name": "Jugo",
                  "salePriceCentavos": 1800,
                  "saleCategory": "No existe"
                }
                """))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("POS_SALE_CATEGORY_NOT_FOUND"));
  }

  private void createSaleCategory(String token, long hqId, String name) throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-categories",
                token,
                "{\"name\": \"%s\"}".formatted(name)))
        .andExpect(status().isCreated());
  }

  private String enrollDevice(String staffToken, long hqId, String deviceName) throws Exception {
    MvcResult codeResult =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/enrollment-codes",
                    staffToken,
                    "{\"headquarterId\": %d}".formatted(hqId)))
            .andExpect(status().isOk())
            .andReturn();
    String code = JsonPath.read(codeResult.getResponse().getContentAsString(), "$.code");
    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll",
                    """
                    {
                      "enrollmentCode": "%s",
                      "devicePublicId": "%s",
                      "deviceName": "%s",
                      "appVersion": "1.0.0"
                    }
                    """
                        .formatted(code, UUID.randomUUID(), deviceName)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");
  }

  private void putPosSettings(String token, long hqId) throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-settings",
                token,
                """
                {
                  "currency": "MXN",
                  "catalogStaleWarnHours": 24,
                  "catalogStaleBlockHours": 72,
                  "openAmountCategories": ["MISC"],
                  "defaultNegativeStockLimit": 10
                }
                """))
        .andExpect(status().isOk());
  }

  private long createHeadquarter(String token, String name) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/headquarters",
                        """
                        {"name": "%s", "address": "Addr", "description": "d"}
                        """
                            .formatted(name))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    Number n = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-pos-dev-cat-" + UUID.randomUUID() + "@mail.com";
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
