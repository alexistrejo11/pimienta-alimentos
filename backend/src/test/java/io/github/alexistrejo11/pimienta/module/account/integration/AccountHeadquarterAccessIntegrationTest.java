package io.github.alexistrejo11.pimienta.module.account.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
  void managerCanReadInventoryLocationsWithinAssignedHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "ACL-INV1-" + UUID.randomUUID());
    putPosSettings(admin.token(), hq1);

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/inventory/locations?type=POS&page=0&size=20", manager.token()))
        .andExpect(status().isOk());
  }

  @Test
  void managerCanReadInventoryStockWithinAssignedHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "ACL-STK1-" + UUID.randomUUID());
    putPosSettings(admin.token(), hq1);

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/inventory/stock?page=0&size=20", manager.token()))
        .andExpect(status().isOk());
  }

  @Test
  void managerCanReadTalentButNotUserManagement() throws Exception {
    TokenPair manager = obtainToken(Set.of(Role.MANAGER));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees?page=0&size=10", manager.token()))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/users/management?page=0&size=10", manager.token()))
        .andExpect(status().isForbidden());
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
}
