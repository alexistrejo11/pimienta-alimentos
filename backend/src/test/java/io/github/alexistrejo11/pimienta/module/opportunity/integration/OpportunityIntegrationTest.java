package io.github.alexistrejo11.pimienta.module.opportunity.integration;

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
class OpportunityIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void list_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/opportunities?page=0&size=10"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void manager_cannotAccessOrganizations() throws Exception {
    String managerToken = obtainToken(Set.of(Role.MANAGER));
    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/organizations?page=0&size=10", managerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void admin_createOpportunity_andWalkLifecycle() throws Exception {
    String adminToken = obtainToken(Set.of(Role.ADMIN));

    MvcResult orgCreated =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/organizations",
                    adminToken,
                    """
                    {
                      "name":"IPN ESFM",
                      "kind":"PUBLIC_INSTITUTION",
                      "notes":"Zacatenco",
                      "contactName":"Comité",
                      "contactEmail":"esfm@example.com",
                      "contactPhone":"+525555550001"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("IPN ESFM"))
            .andExpect(jsonPath("$.kind").value("PUBLIC_INSTITUTION"))
            .andReturn();
    long orgId =
        ((Number) JsonPath.read(orgCreated.getResponse().getContentAsString(), "$.id")).longValue();

    MvcResult oppCreated =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/opportunities",
                    adminToken,
                    """
                    {
                      "organizationId":%d,
                      "title":"Cafetería ESFM 2027",
                      "sourceUrl":"https://example.com/convocatoria",
                      "externalReference":"ESFM-2027",
                      "publishedOn":"2026-10-29",
                      "submissionDeadline":"2026-11-24",
                      "notes":"Caso de prueba F1"
                    }
                    """
                        .formatted(orgId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.organizationId").value((int) orgId))
            .andReturn();
    long oppId =
        ((Number) JsonPath.read(oppCreated.getResponse().getContentAsString(), "$.id")).longValue();

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/submit-for-review", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/start-preparing", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PREPARING"));

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/mark-submitted", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"))
        .andExpect(jsonPath("$.submittedAt").isNotEmpty());

    MvcResult won =
        mockMvc
            .perform(
                AccountTestRequests.postBearer(
                    "/api/v1/opportunities/" + oppId + "/mark-won", adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("WON"))
            .andExpect(jsonPath("$.closedAt").isNotEmpty())
            .andExpect(jsonPath("$.contractId").isNumber())
            .andReturn();
    long contractId =
        ((Number) JsonPath.read(won.getResponse().getContentAsString(), "$.contractId"))
            .longValue();

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/contracts/" + contractId, adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category").value("INSTITUTIONAL"))
        .andExpect(jsonPath("$.name").value("Cafetería ESFM 2027"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/opportunities?organizationId=" + orgId + "&page=0&size=10", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value((int) oppId))
        .andExpect(jsonPath("$.items[0].status").value("WON"));
  }

  @Test
  void invalidTransition_returns400() throws Exception {
    String adminToken = obtainToken(Set.of(Role.ADMIN));
    long orgId = createOrg(adminToken);
    long oppId = createOpportunity(adminToken, orgId);

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/mark-submitted", adminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void markSubmitted_blockedByMandatoryRequirement_untilDone() throws Exception {
    String adminToken = obtainToken(Set.of(Role.ADMIN));
    long orgId = createOrg(adminToken);
    long oppId = createOpportunity(adminToken, orgId);

    MvcResult reqCreated =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/opportunities/" + oppId + "/requirements",
                    adminToken,
                    """
                    {
                      "title":"Acta constitutiva",
                      "category":"LEGAL",
                      "mandatory":true
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.mandatory").value(true))
            .andReturn();
    long reqId =
        ((Number) JsonPath.read(reqCreated.getResponse().getContentAsString(), "$.id")).longValue();

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/submit-for-review", adminToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/start-preparing", adminToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/mark-submitted", adminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.context.pendingMandatory").value(1));

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/opportunities/" + oppId + "/requirements/" + reqId + "/status",
                adminToken,
                """
                {"status":"DONE"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"));

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/mark-submitted", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));
  }

  @Test
  void admin_canManageProposals_untilSubmitted() throws Exception {
    String adminToken = obtainToken(Set.of(Role.ADMIN));
    long orgId = createOrg(adminToken);
    long oppId = createOpportunity(adminToken, orgId);

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/opportunities/" + oppId + "/proposals",
                    adminToken,
                    """
                    {"kind":"COMBINED","notes":"Paquete técnico-económico"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.kind").value("COMBINED"))
            .andExpect(jsonPath("$.notes").value("Paquete técnico-económico"))
            .andReturn();
    long proposalId =
        ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/opportunities/" + oppId + "/proposals/" + proposalId,
                adminToken,
                """
                {"kind":"TECHNICAL","notes":"Solo técnica"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.kind").value("TECHNICAL"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/opportunities/" + oppId + "/proposals", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value((int) proposalId));

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/submit-for-review", adminToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/start-preparing", adminToken))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/opportunities/" + oppId + "/mark-submitted", adminToken))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/opportunities/" + oppId + "/proposals",
                adminToken,
                """
                {"kind":"ECONOMIC","notes":"tarde"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  private long createOrg(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/organizations",
                    token,
                    """
                    {"name":"Org %s","kind":"OTHER"}
                    """
                        .formatted(UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  private long createOpportunity(String token, long orgId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/opportunities",
                    token,
                    """
                    {"organizationId":%d,"title":"Opp %s"}
                    """
                        .formatted(orgId, UUID.randomUUID())))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  private String obtainToken(Set<Role> roles) throws Exception {
    String email = "it-opp-" + UUID.randomUUID() + "@mail.com";
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
    return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
  }
}
