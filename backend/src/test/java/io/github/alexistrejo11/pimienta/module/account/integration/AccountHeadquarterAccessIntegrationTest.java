package io.github.alexistrejo11.pimienta.module.account.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
class AccountHeadquarterAccessIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void managerForbiddenOnOtherHeadquarterPosCatalog() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "ACL-HQ1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "ACL-HQ2-" + UUID.randomUUID());

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/" + hq1 + "/pos-catalog", manager.token()))
        .andExpect(status().isOk());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/" + hq2 + "/pos-catalog", manager.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
  }

  @Test
  void usersMeIncludesAssignedHeadquarterIds() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "ACL-ME-" + UUID.randomUUID());
    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/me", manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignedHeadquarterIds[0]").value((int) hq1));
  }

  @Test
  void managerInventoryLocationsScopedToAssignedHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "ACL-INV1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "ACL-INV2-" + UUID.randomUUID());
    putPosSettings(admin.token(), hq1);
    putPosSettings(admin.token(), hq2);

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/inventory/locations?type=POS&page=0&size=20", manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].headquarterId").value((int) hq1));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/inventory/locations?type=POS&headquarterId="
                    + hq2
                    + "&page=0&size=20",
                manager.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
  }

  @Test
  void managerInventoryStockScopedToAssignedHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "ACL-STK1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "ACL-STK2-" + UUID.randomUUID());
    putPosSettings(admin.token(), hq1);
    putPosSettings(admin.token(), hq2);

    long itemId = createItem(admin.token(), "ACL-SKU-" + UUID.randomUUID());
    long loc1 = findPosLocationId(admin.token(), hq1);
    long loc2 = findPosLocationId(admin.token(), hq2);
    createInitialStock(admin.token(), itemId, loc1, 5);
    createInitialStock(admin.token(), itemId, loc2, 9);

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/inventory/stock?itemId=" + itemId + "&page=0&size=20", manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].locationId").value((int) loc1))
        .andExpect(jsonPath("$.items[0].availableQuantity").value(5));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/inventory/stock?itemId=" + itemId + "&headquarterId="
                    + hq2
                    + "&page=0&size=20",
                manager.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
  }

  private record TokenPair(String token, Long userId) {}

  private TokenPair obtainToken(Set<Role> roles) throws Exception {
    String email = "it-acl-" + UUID.randomUUID() + "@mail.com";
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

  private long createHeadquarter(String token, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/headquarters",
                    token,
                    """
                    {"name":"%s","address":"Test 1","description":"ACL IT"}
                    """
                        .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
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

  private long createItem(String token, String sku) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/inventory/items",
                    token,
                    """
                    {
                      "sku": "%s",
                      "name": "ACL item",
                      "description": "ACL IT",
                      "costPrice": 10.00,
                      "salePrice": 15.00,
                      "category": "CONSUMABLE",
                      "unit": "PIECE",
                      "reorderPoint": 0,
                      "reorderQuantity": 0
                    }
                    """
                        .formatted(sku)))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  private long findPosLocationId(String token, long hqId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.getBearer(
                    "/api/v1/inventory/locations?type=POS&headquarterId="
                        + hqId
                        + "&page=0&size=5",
                    token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.items[0].id"))
        .longValue();
  }

  private void createInitialStock(String token, long itemId, long locationId, int quantity)
      throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/inventory/stock",
                token,
                """
                {"itemId": %d, "locationId": %d, "initialQuantity": %d}
                """
                    .formatted(itemId, locationId, quantity)))
        .andExpect(status().isCreated());
  }
}
