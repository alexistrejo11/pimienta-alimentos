package io.github.alexistrejo11.pimienta.module.supplier.integration;

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
class SupplierIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void list_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/suppliers?page=0&size=10")).andExpect(status().isUnauthorized());
  }

  @Test
  void createListDelete_flow_managerScopedToHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "SUP-HQ1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "SUP-HQ2-" + UUID.randomUUID());

    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), hq1);

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/suppliers",
                    manager.token(),
                    body("Marinela Centro", "Juan Pérez", "+528110000001", "Marinela", hq1)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.brand").value("Marinela"))
            .andReturn();
    long supplierId =
        ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/suppliers?headquarterId=" + hq1 + "&page=0&size=20", manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value((int) supplierId));

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/suppliers",
                manager.token(),
                body("Otro", "Ana", "+528110000002", "Barcel", hq2)))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/suppliers/" + supplierId, manager.token()))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/suppliers?headquarterId=" + hq1 + "&page=0&size=20", manager.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void director_canUpdateSupplierAtAssignedHeadquarter() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq = createHeadquarter(admin.token(), "SUP-DIR-" + UUID.randomUUID());
    TokenPair director = obtainToken(Set.of(Role.DIRECTOR));
    assignHeadquarters(admin.token(), director.userId(), hq);

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/suppliers",
                    director.token(),
                    body("Proveedor", "Contacto", "+528110000003", "Sabritas", hq)))
            .andExpect(status().isCreated())
            .andReturn();
    long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

    mockMvc
        .perform(AccountTestRequests.putJsonBearer(
            "/api/v1/suppliers/" + id,
            director.token(),
            body("Proveedor SA", "Contacto", "+528110000004", "Sabritas", hq)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Proveedor SA"));
  }

  private static String body(String name, String contact, String phone, String brand, long hqId) {
    return """
        {"name":"%s","contactName":"%s","phone":"%s","brand":"%s","headquarterIds":[%d]}
        """
        .formatted(name, contact, phone, brand, hqId);
  }

  private record TokenPair(String token, Long userId) {}

  private TokenPair obtainToken(Set<Role> roles) throws Exception {
    String email = "it-sup-" + UUID.randomUUID() + "@mail.com";
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
                    {"name":"%s","address":"Test 1","description":"Supplier IT"}
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
}
