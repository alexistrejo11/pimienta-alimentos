package io.github.alexistrejo11.pimienta.module.contract.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
class ContractManagerIntegrationTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @Test
  void listContracts_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/contracts")).andExpect(status().isUnauthorized());
  }

  @Test
  void getContract_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/contracts/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void createContract_validation_blankName_returns400() throws Exception {
    String token = obtainAccessToken();
    String body =
        """
            {
              "name": "   ",
              "category": "SUPPLIER",
              "termKind": "FIXED_TERM",
              "effectiveStart": "2025-01-01",
              "effectiveEnd": "2026-01-01",
              "documentUrl": "https://example.com/a.pdf"
            }
            """;

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/contracts", body)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void createContract_validation_malformedJson_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/contracts")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void createContract_domain_fixedTermMissingEnd_returns400() throws Exception {
    String token = obtainAccessToken();
    ObjectNode n = JSON.createObjectNode();
    n.put("name", "IT no end " + UUID.randomUUID());
    n.put("category", "SUPPLIER");
    n.put("termKind", "FIXED_TERM");
    n.put("effectiveStart", "2025-01-01");
    n.put("documentUrl", "https://example.com/x.pdf");

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/contracts", JSON.writeValueAsString(n))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void createContract_domain_indefiniteWithEnd_returns400() throws Exception {
    String token = obtainAccessToken();
    ObjectNode n = JSON.createObjectNode();
    n.put("name", "IT indef " + UUID.randomUUID());
    n.put("category", "OTHER");
    n.put("termKind", "INDEFINITE");
    n.put("effectiveStart", "2025-01-01");
    n.put("effectiveEnd", "2026-01-01");
    n.put("documentUrl", "https://example.com/x.pdf");

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/contracts", JSON.writeValueAsString(n))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void createContract_domain_agreedValueWithoutCurrency_returns400() throws Exception {
    String token = obtainAccessToken();
    ObjectNode n = JSON.createObjectNode();
    n.put("name", "IT money " + UUID.randomUUID());
    n.put("category", "CUSTOMER");
    n.put("termKind", "FIXED_TERM");
    n.put("effectiveStart", "2025-01-01");
    n.put("effectiveEnd", "2026-01-01");
    n.put("documentUrl", "https://example.com/x.pdf");
    n.put("agreedValue", "1000.00");

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/contracts", JSON.writeValueAsString(n))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void createContract_domain_employeeCategoryWithoutEmployee_returns400() throws Exception {
    String token = obtainAccessToken();
    ObjectNode n = JSON.createObjectNode();
    n.put("name", "IT emp " + UUID.randomUUID());
    n.put("category", "EMPLOYEE");
    n.put("termKind", "FIXED_TERM");
    n.put("effectiveStart", "2025-01-01");
    n.put("effectiveEnd", "2026-01-01");
    n.put("documentUrl", "https://example.com/x.pdf");

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/contracts", JSON.writeValueAsString(n))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void createContract_domain_opportunityNotFound_returns404() throws Exception {
    String token = obtainAccessToken();
    ObjectNode n = JSON.createObjectNode();
    n.put("name", "IT opp " + UUID.randomUUID());
    n.put("category", "SUPPLIER");
    n.put("opportunityId", 9_999_999_999L);
    n.put("termKind", "FIXED_TERM");
    n.put("effectiveStart", "2025-01-01");
    n.put("effectiveEnd", "2026-01-01");
    n.put("documentUrl", "https://example.com/x.pdf");

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/contracts", JSON.writeValueAsString(n))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("OPPORTUNITY_NOT_FOUND"));
  }

  @Test
  void createGetListUpdateRenewExtendDelete_flow() throws Exception {
    String token = obtainAccessToken();
    String name = "IT-flow-" + UUID.randomUUID();
    String createBody = fixedTermSupplierJson(name);

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/contracts", createBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(name))
            .andExpect(jsonPath("$.category").value("SUPPLIER"))
            .andExpect(jsonPath("$.termKind").value("FIXED_TERM"))
            .andExpect(jsonPath("$.effectiveEnd").value("2026-01-01"))
            .andReturn();

    long id = extractContractId(created.getResponse().getContentAsString());

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/contracts/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/contracts?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata").exists());

    ObjectNode update = JSON.createObjectNode();
    update.put("name", name + "-updated");
    update.put("description", "updated desc");
    update.put("category", "SUPPLIER");
    update.put("termKind", "FIXED_TERM");
    update.put("effectiveStart", "2025-01-01");
    update.put("effectiveEnd", "2026-01-01");
    update.put("documentUrl", "https://example.com/updated.pdf");
    update.put("termsAndConditions", "Net 45");
    update.put("referenceCode", "REF-U");
    update.put("renewalCycleMonths", 12);

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/contracts/" + id, token, JSON.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(name + "-updated"));

    mockMvc
        .perform(AccountTestRequests.postBearer("/api/v1/contracts/" + id + "/renew", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.extensionCount").value(1));

    String extendBody = "{\"newEnd\":\"2028-06-30\"}";
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/contracts/" + id + "/extend", token, extendBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.effectiveEnd").value("2028-06-30"));

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/contracts/" + id, token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/contracts/" + id, token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_FOUND"));
  }

  @Test
  void getContract_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/contracts/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("CONTRACT_NOT_FOUND"));
  }

  @Test
  void getContract_invalidPathId_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/contracts/not-a-number", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
  }

  @Test
  void renewContract_whenIndefinite_returns400() throws Exception {
    String token = obtainAccessToken();
    ObjectNode n = JSON.createObjectNode();
    n.put("name", "IT-indef-" + UUID.randomUUID());
    n.put("category", "PARTNER");
    n.put("termKind", "INDEFINITE");
    n.put("effectiveStart", "2025-01-01");
    n.put("documentUrl", "https://example.com/indef.pdf");

    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/contracts", JSON.writeValueAsString(n))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractContractId(r.getResponse().getContentAsString());

    mockMvc
        .perform(AccountTestRequests.postBearer("/api/v1/contracts/" + id + "/renew", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void extendContract_validationMissingNewEnd_returns400() throws Exception {
    String token = obtainAccessToken();
    String createBody = fixedTermSupplierJson("IT-ext-" + UUID.randomUUID());
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/contracts", createBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractContractId(r.getResponse().getContentAsString());

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/contracts/" + id + "/extend", token, "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void extendContract_newEndNotAfterCurrent_returns400() throws Exception {
    String token = obtainAccessToken();
    String createBody = fixedTermSupplierJson("IT-ext2-" + UUID.randomUUID());
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/contracts", createBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractContractId(r.getResponse().getContentAsString());

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/contracts/" + id + "/extend",
                token,
                "{\"newEnd\":\"2025-06-01\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  private static long extractContractId(String responseJson) {
    Number n = JsonPath.read(responseJson, "$.id");
    return n.longValue();
  }

  private String fixedTermSupplierJson(String name) throws Exception {
    ObjectNode n = JSON.createObjectNode();
    n.put("name", name);
    n.put("description", "integration");
    n.put("category", "SUPPLIER");
    n.put("termKind", "FIXED_TERM");
    n.put("effectiveStart", "2025-01-01");
    n.put("effectiveEnd", "2026-01-01");
    n.put("documentUrl", "https://example.com/contracts/it.pdf");
    n.put("termsAndConditions", "Net 30");
    n.put("referenceCode", "IT-REF");
    n.put("renewalCycleMonths", 12);
    return JSON.writeValueAsString(n);
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-contract-" + UUID.randomUUID() + "@mail.com";
    String phone = "+52" + String.format("%010d", Math.abs(ThreadLocalRandom.current().nextLong()) % 10_000_000_000L);
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
