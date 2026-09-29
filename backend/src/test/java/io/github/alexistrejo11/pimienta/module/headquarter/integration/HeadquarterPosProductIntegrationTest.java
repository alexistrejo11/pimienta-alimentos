package io.github.alexistrejo11.pimienta.module.headquarter.integration;

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
class HeadquarterPosProductIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void posProducts_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/headquarters/1/pos-products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Agua\",\"salePrice\":15.00,\"posSaleCategoryId\":1}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void managerCanCreatePosProductOnAssignedHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hqId = createHeadquarter(admin.token(), "POS-PROD-" + UUID.randomUUID());
    long categoryId = createSaleCategory(admin.token(), hqId, "Bebidas");

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hqId);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-products",
                manager.token(),
                """
                {
                  "name": "Agua natural",
                  "salePrice": 15.00,
                  "posSaleCategoryId": %d,
                  "stockPolicy": "NOT_CONTROLLED"
                }
                """
                    .formatted(categoryId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Agua natural"))
        .andExpect(jsonPath("$.headquarterId").value(hqId))
        .andExpect(jsonPath("$.posSaleCategoryId").value(categoryId))
        .andExpect(jsonPath("$.saleCategory").value("Bebidas"))
        .andExpect(jsonPath("$.salePrice").value(15.00))
        .andExpect(jsonPath("$.stockPolicy").value("NOT_CONTROLLED"))
        .andExpect(jsonPath("$.trackStock").value(false));
  }

  @Test
  void managerForbiddenOnOtherHeadquarterPosProducts() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "POS-PROD-1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "POS-PROD-2-" + UUID.randomUUID());
    long categoryId = createSaleCategory(admin.token(), hq2, "Snacks");

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/headquarters/" + hq2 + "/pos-products",
                manager.token(),
                """
                {
                  "name": "Papas",
                  "salePrice": 20.00,
                  "posSaleCategoryId": %d
                }
                """
                    .formatted(categoryId)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
  }

  private record TokenPair(String token, Long userId) {}

  private TokenPair obtainToken(Set<Role> roles) throws Exception {
    String email = "it-pos-prod-" + UUID.randomUUID() + "@mail.com";
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
                    {"name":"%s","address":"Test 1","description":"POS product IT"}
                    """
                        .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  private long createSaleCategory(String token, long hqId, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/headquarters/" + hqId + "/pos-categories",
                    token,
                    "{\"name\": \"%s\"}".formatted(name)))
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
}
