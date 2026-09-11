package io.github.alexistrejo11.pimienta.module.pos.integration;

import static org.hamcrest.Matchers.hasSize;
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
class PosAdminOperatorIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void managerListsOnlyOperatorsForAssignedHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "OP-HQ1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "OP-HQ2-" + UUID.randomUUID());

    long op1 = createOperator(admin.token(), hq1, "Cajera HQ1");
    createOperator(admin.token(), hq2, "Cajera HQ2");

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/admin/operators?page=0&size=50", manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].id").value((int) op1));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/admin/operators/" + op1, manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value((int) op1));
  }

  @Test
  void managerForbiddenOnOperatorOutsideAssignedHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "OP-ACL1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "OP-ACL2-" + UUID.randomUUID());
    long otherOp = createOperator(admin.token(), hq2, "Other HQ cashier");

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/pos/admin/operators/" + otherOp, manager.token()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
  }

  @Test
  void createOperator_withoutHeadquarterIds_returns400() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/admin/operators",
                admin.token(),
                """
                {
                  "displayName": "No HQ",
                  "posRole": "CASHIER",
                  "pin": "1234",
                  "headquarterIds": []
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void managerCannotCreateOperatorForOtherHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "OP-CR1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "OP-CR2-" + UUID.randomUUID());

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/admin/operators",
                manager.token(),
                """
                {
                  "displayName": "Foreign HQ",
                  "posRole": "CASHIER",
                  "pin": "1234",
                  "headquarterIds": [%d]
                }
                """
                    .formatted(hq2)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
  }

  private record TokenPair(String token, Long userId) {}

  private TokenPair obtainToken(Set<Role> roles) throws Exception {
    String email = "it-pos-op-" + UUID.randomUUID() + "@mail.com";
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
                    {"name":"%s","address":"Test 1","description":"OP IT"}
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
}
