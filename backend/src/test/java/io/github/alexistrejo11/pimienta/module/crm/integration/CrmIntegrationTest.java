package io.github.alexistrejo11.pimienta.module.crm.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.XlsxTestFiles;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class CrmIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void opportunities_search_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/opportunities")).andExpect(status().isUnauthorized());
  }

  @Test
  void opportunities_create_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/opportunities", minimalCreateOpportunityJson("x", "co")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void opportunities_create_validation_blankTitle_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/opportunities", minimalCreateOpportunityJson("   ", "Acme IT"))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void opportunities_create_malformedJson_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/opportunities")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void opportunities_getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/opportunities/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("OPPORTUNITY_NOT_FOUND"));
  }

  @Test
  void opportunities_getById_invalidPath_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/opportunities/not-id", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
  }

  @Test
  void opportunities_importEmptyFile_returns400() throws Exception {
    String token = obtainAccessToken();
    MockMultipartFile file =
        new MockMultipartFile("file", "empty.xlsx", "application/octet-stream", new byte[0]);
    mockMvc
        .perform(
            multipart("/api/v1/opportunities/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
  }

  @Test
  void opportunities_importDryRun_doesNotPersist() throws Exception {
    String token = obtainAccessToken();
    String title = "IT-OPP-DRY-" + UUID.randomUUID();
    MockMultipartFile file =
        XlsxTestFiles.multipart("opp.xlsx", opportunityHeaders(), opportunityCreateCells(title, "Desc"));

    mockMvc
        .perform(
            multipart("/api/v1/opportunities/import")
                .file(file)
                .param("dryRun", "true")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dryRun").value(true))
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/opportunities?titleContains="
                    + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8)
                    + "&page=0&size=20",
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metadata.totalElements").value(0));
  }

  @Test
  void opportunities_importOneInvalidRow_writesNothing() throws Exception {
    String token = obtainAccessToken();
    String title = "IT-OPP-OK-" + UUID.randomUUID();
    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "opp.xlsx",
            opportunityHeaders(),
            opportunityCreateCells(title, "Desc"),
            new Object[] {
              999999999L,
              "nope",
              "",
              "",
              "",
              "",
              "",
              "",
              "",
              null,
              null,
              "",
              "",
              null,
              ""
            });

    mockMvc
        .perform(
            multipart("/api/v1/opportunities/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.errors", hasSize(1)));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/opportunities?titleContains="
                    + java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8)
                    + "&page=0&size=20",
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metadata.totalElements").value(0));
  }

  @Test
  void opportunities_importBlankDescriptionOnUpdate_keepsExisting() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String title = "IT-OPP-PATCH-" + suffix;
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/opportunities",
                        minimalCreateOpportunityJson(title, suffix))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    Object[] update = opportunityCreateCells(title + "-renamed", "");
    update[0] = id;

    MockMultipartFile file = XlsxTestFiles.multipart("opp.xlsx", opportunityHeaders(), update);

    mockMvc
        .perform(
            multipart("/api/v1/opportunities/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/opportunities/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value(title + "-renamed"))
        .andExpect(jsonPath("$.description").value("CRM IT"));
  }

  @Test
  void opportunities_export_returnsSpreadsheetHeaders() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/opportunities/export?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(HttpHeaders.CONTENT_DISPOSITION, containsString("oportunidades_reporte.xlsx")))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_TYPE,
                    containsString(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
  }

  @Test
  void opportunities_moveDiscoveryTwice_returns400InvalidArgument() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/opportunities", minimalCreateOpportunityJson("IT dup " + suffix, suffix))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            post("/api/v1/opportunities/" + id + "/pipeline/discovery")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/api/v1/opportunities/" + id + "/pipeline/discovery")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
  }

  @Test
  void opportunities_lose_blankReason_returns400Validation() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/opportunities", minimalCreateOpportunityJson("IT lose " + suffix, suffix))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/opportunities/" + id + "/lose", "{\"reason\":\"\"}")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void opportunities_pipeline_win_task_export_delete_flow() throws Exception {
    String token = obtainAccessToken();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String title = "IT-OPP-" + suffix;

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/opportunities", minimalCreateOpportunityJson(title, suffix))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.status").value("NEW"))
            .andReturn();
    long oppId = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.patchJsonBearer(
                "/api/v1/opportunities/" + oppId,
                token,
                "{\"title\": \"" + title + " updated\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value(title + " updated"));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/opportunities/" + oppId + "/summary", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.opportunity.id").value(oppId))
        .andExpect(jsonPath("$.taskCount").exists());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/opportunities?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata.pageNumber").value(0));

    mockMvc
        .perform(
            post("/api/v1/opportunities/" + oppId + "/pipeline/discovery")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISCOVERY"));

    mockMvc
        .perform(
            post("/api/v1/opportunities/" + oppId + "/pipeline/proposal")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PROPOSAL"));

    mockMvc
        .perform(
            post("/api/v1/opportunities/" + oppId + "/pipeline/negotiation")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("NEGOTIATION"));

    String projectCode = "IT-WIN-" + suffix;
    String winBody =
        """
            {
              "projectCode": "%s",
              "projectName": "Delivery project",
              "description": "From IT",
              "clientId": 90001,
              "type": "CONSULTING",
              "priority": "MEDIUM",
              "contractedValue": 15000.50,
              "estimatedCost": 8000.00
            }
            """
            .formatted(projectCode);

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/opportunities/" + oppId + "/win", winBody)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("WON"))
        .andExpect(jsonPath("$.convertedProjectId").exists());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/opportunities/" + oppId + "/tasks?page=0&size=10", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/opportunities/" + oppId + "/tasks", "{\"title\":\"Opp task\"}")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Opp task"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/opportunities/export?titleContains=" + java.net.URLEncoder.encode("IT-OPP-", java.nio.charset.StandardCharsets.UTF_8) + "&page=0&size=10",
                token))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("oportunidades_reporte.xlsx")));

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/opportunities/" + oppId, token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/opportunities/" + oppId, token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("OPPORTUNITY_NOT_FOUND"));
  }

  @Test
  void projects_search_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/projects")).andExpect(status().isUnauthorized());
  }

  @Test
  void projects_create_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/projects", minimalCreateProjectJson("X", "N")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void projects_getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/projects/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("PROJECT_NOT_FOUND"));
  }

  @Test
  void projects_listMilestones_projectNotFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/projects/999999999/milestones?page=0&size=10", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("PROJECT_NOT_FOUND"));
  }

  @Test
  void projects_duplicateProjectCode_returns409() throws Exception {
    String token = obtainAccessToken();
    String code = "IT-DUP-" + UUID.randomUUID().toString().substring(0, 8);
    String body = minimalCreateProjectJson(code, "First " + code);

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/projects", body)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/projects", body)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("CONFLICT"));
  }

  @Test
  void projects_update_progressOutOfRange_returns400() throws Exception {
    String token = obtainAccessToken();
    String code = "IT-PRG-" + UUID.randomUUID().toString().substring(0, 8);
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/projects", minimalCreateProjectJson(code, "Prog " + code))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.patchJsonBearer(
                "/api/v1/projects/" + id, token, "{\"progressPercent\": 150}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
  }

  @Test
  void projects_milestoneLifecycle_andHoldCompleteArchive_flow() throws Exception {
    String token = obtainAccessToken();
    String code = "IT-FLOW-" + UUID.randomUUID().toString().substring(0, 8);
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/projects", minimalCreateProjectJson(code, "Flow " + code))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PLANNING"))
            .andReturn();
    long projectId = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/projects/" + projectId + "/summary", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.project.id").value(projectId))
        .andExpect(jsonPath("$.milestoneCount").exists());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/projects?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/activate")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    String milestoneBody =
        """
            {
              "name": "Kickoff",
              "description": "Start",
              "billingAmount": 1000.00,
              "sortOrder": 0
            }
            """;

    MvcResult ms =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/projects/" + projectId + "/milestones", milestoneBody)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Kickoff"))
            .andReturn();
    long milestoneId = extractLongId(ms.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/projects/" + projectId + "/milestones?page=0&size=10", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/projects/" + projectId + "/milestones/" + milestoneId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(milestoneId));

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/milestones/" + milestoneId + "/start")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/milestones/" + milestoneId + "/complete")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/milestones/" + milestoneId + "/billed")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.billed").value(true));

    mockMvc
        .perform(
            AccountTestRequests.patchJsonBearer(
                "/api/v1/projects/" + projectId + "/milestones/" + milestoneId,
                token,
                "{\"name\": \"Kickoff revised\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Kickoff revised"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/projects/" + projectId + "/tasks?page=0&size=5", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/projects/" + projectId + "/hold", "{\"reason\":\"Waiting on client\"}")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ON_HOLD"));

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/activate")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/complete")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));

    mockMvc
        .perform(
            post("/api/v1/projects/" + projectId + "/archive")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"));

    mockMvc
        .perform(
            AccountTestRequests.deleteBearer(
                "/api/v1/projects/" + projectId + "/milestones/" + milestoneId, token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/projects/" + projectId + "/milestones/" + milestoneId, token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("PROJECT_MILESTONE_NOT_FOUND"));
  }

  private static String[] opportunityHeaders() {
    return new String[] {
      "ID",
      "Title",
      "Description",
      "ContactName",
      "ContactEmail",
      "ContactPhone",
      "CompanyName",
      "CompanyLocation",
      "Industry",
      "EstimatedValue",
      "ProbabilityPercent",
      "Source",
      "ExpectedCloseDate",
      "AssignedSalesmanId",
      "Status"
    };
  }

  private static Object[] opportunityCreateCells(String title, String description) {
    return new Object[] {
      null,
      title,
      description,
      "Jane Doe",
      "jane@example.com",
      "+52-55-0000-0000",
      "Acme SA",
      "Monterrey",
      "Food",
      5000,
      20,
      "INBOUND",
      "2026-12-31",
      null,
      ""
    };
  }

  private static String minimalCreateOpportunityJson(String title, String companySuffix) {
    return """
        {
          "title": "%s",
          "description": "CRM IT",
          "contactName": "Jane Doe",
          "contactEmail": "jane+%s@example.com",
          "contactPhone": "+52-55-0000-0000",
          "companyName": "Acme %s SA",
          "companyLocation": "Monterrey",
          "industry": "Food",
          "estimatedValue": 5000.00,
          "probabilityPercent": 20,
          "source": "INBOUND",
          "expectedCloseDate": "2026-12-31"
        }
        """
        .formatted(title, companySuffix, companySuffix);
  }

  private static String minimalCreateProjectJson(String projectCode, String projectName) {
    return """
        {
          "projectCode": "%s",
          "projectName": "%s",
          "description": "IT project",
          "clientId": 424242,
          "type": "SOFTWARE_DEVELOPMENT",
          "priority": "HIGH",
          "contractedValue": 12000.00,
          "estimatedCost": 6000.00
        }
        """
        .formatted(projectCode, projectName);
  }

  private static long extractLongId(String json, String path) {
    Number n = JsonPath.read(json, path);
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-crm-" + UUID.randomUUID() + "@mail.com";
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
