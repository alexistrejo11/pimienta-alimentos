package io.github.alexistrejo11.pimienta.module.task.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class TaskManagerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void search_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/tasks")).andExpect(status().isUnauthorized());
  }

  @Test
  void create_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/tasks", minimalCreateJson("IT task")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void create_validation_blankTitle_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/tasks", minimalCreateJson("   "))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void create_validation_checklistBlankDescription_returns400() throws Exception {
    String token = obtainAccessToken();
    String body =
        """
            {
              "title": "With bad checklist",
              "checklist": [{"description": "   ", "displayOrder": 0}]
            }
            """;
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/tasks", body)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void create_validation_malformedJson_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void search_create_get_updateStatus_assign_toggle_export_delete_flow() throws Exception {
    String token = obtainAccessToken();
    String title = "IT-TASK-" + UUID.randomUUID();

    String createJson =
        """
            {
              "title": "%s",
              "description": "Integration flow",
              "priority": "HIGH",
              "checklist": [
                {"description": "Step one", "displayOrder": 0},
                {"description": "Step two", "displayOrder": 1}
              ]
            }
            """
            .formatted(title);

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/tasks", createJson)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value(title))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.checklist").isArray())
            .andExpect(jsonPath("$.checklist", hasSize(2)))
            .andReturn();

    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata.totalElements").exists())
        .andExpect(jsonPath("$.metadata.pageNumber").value(0));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks?status=PENDING&page=0&size=10", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.progressPercent").value(0.0));

    mockMvc
        .perform(
            AccountTestRequests.patchJsonBearer(
                "/api/v1/tasks/" + id + "/status",
                token,
                "{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

    mockMvc
        .perform(
            AccountTestRequests.patchJsonBearer(
                "/api/v1/tasks/" + id + "/status",
                token,
                "{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.completedAt").exists());

    mockMvc
        .perform(
            AccountTestRequests.patchJsonBearer(
                "/api/v1/tasks/" + id + "/assign",
                token,
                "{\"employeeId\": 424242}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assignedToId").value(424242));

    mockMvc
        .perform(
            patch("/api/v1/tasks/" + id + "/checklist/0/toggle")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.checklist[0].completed").value(true));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks/export?page=0&size=50", token))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("tareas_reporte.xlsx")))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_TYPE,
                    containsString(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/tasks/" + id, token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks/" + id, token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("TASK_NOT_FOUND"));
  }

  @Test
  void getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("TASK_NOT_FOUND"));
  }

  @Test
  void getById_invalidPath_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks/not-numeric", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
  }

  @Test
  void updateStatus_validation_missingStatus_returns400() throws Exception {
    String token = obtainAccessToken();
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/tasks", minimalCreateJson("Status val"))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.patchJsonBearer(
                "/api/v1/tasks/" + id + "/status", token, "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void assign_validation_missingEmployeeId_returns400() throws Exception {
    String token = obtainAccessToken();
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/tasks", minimalCreateJson("Assign val"))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.patchJsonBearer(
                "/api/v1/tasks/" + id + "/assign", token, "{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void toggleChecklistItem_unknownOrder_returns404() throws Exception {
    String token = obtainAccessToken();
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/tasks", minimalCreateJson("Toggle val"))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            patch("/api/v1/tasks/" + id + "/checklist/99/toggle")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("TASK_CHECKLIST_ITEM_NOT_FOUND"));
  }

  @Test
  void importTasks_emptyFile_returns400() throws Exception {
    String token = obtainAccessToken();
    MockMultipartFile file =
        new MockMultipartFile("file", "empty.xlsx", "application/octet-stream", new byte[0]);

    mockMvc
        .perform(
            multipart("/api/v1/tasks/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
  }

  @Test
  void importTasks_dryRun_doesNotPersist() throws Exception {
    String token = obtainAccessToken();
    String title = "IT-TASK-DRY-" + UUID.randomUUID();
    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "tasks.xlsx",
            new String[] {"ID", "Title", "Status", "Checklist", "Headquarter"},
            new Object[] {null, title, "", "", ""});

    mockMvc
        .perform(
            multipart("/api/v1/tasks/import")
                .file(file)
                .param("dryRun", "true")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dryRun").value(true))
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks?page=0&size=100", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.title=='" + title + "')]", hasSize(0)));
  }

  @Test
  void importTasks_oneInvalidRow_writesNothing() throws Exception {
    String token = obtainAccessToken();
    String title = "IT-TASK-OK-" + UUID.randomUUID();
    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "tasks.xlsx",
            new String[] {"ID", "Title", "Status", "Checklist", "Headquarter"},
            new Object[] {null, title, "", "", ""},
            new Object[] {999999999L, "nope", "", "", ""});

    mockMvc
        .perform(
            multipart("/api/v1/tasks/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.errors", hasSize(1)));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks?page=0&size=100", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.title=='" + title + "')]", hasSize(0)));
  }

  @Test
  void importTasks_blankTitleOnUpdate_keepsExisting() throws Exception {
    String token = obtainAccessToken();
    String title = "IT-TASK-KEEP-" + UUID.randomUUID();
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/tasks", minimalCreateJson(title))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "tasks.xlsx",
            new String[] {"ID", "Title", "Status", "Checklist", "Headquarter"},
            new Object[] {id, "", "IN_PROGRESS", "", ""});

    mockMvc
        .perform(
            multipart("/api/v1/tasks/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/tasks/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value(title))
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  private static String minimalCreateJson(String title) {
    return "{\"title\": \"" + title.replace("\"", "\\\"") + "\", \"description\": \"\"}";
  }

  private static long extractLongId(String json, String path) {
    Number n = JsonPath.read(json, path);
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-task-" + UUID.randomUUID() + "@mail.com";
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
