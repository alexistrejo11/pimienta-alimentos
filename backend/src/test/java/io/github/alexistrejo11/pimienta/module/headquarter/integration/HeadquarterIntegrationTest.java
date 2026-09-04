package io.github.alexistrejo11.pimienta.module.headquarter.integration;

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
class HeadquarterIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @Test
  void statistics_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/headquarters/statistics")).andExpect(status().isUnauthorized());
  }

  @Test
  void create_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/headquarters", createBody("HQ-X", "addr", "desc")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void create_validation_blankName_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/headquarters", createBody("   ", null, null))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void create_validation_malformedJson_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/headquarters")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void createListGetByIdGetByNameUpdateExportStatistics_flow() throws Exception {
    String token = obtainAccessToken();
    String unique = "IT-HQ-" + UUID.randomUUID();
    String createJson =
        """
            {
              "name": "%s",
              "address": "1200 Industrial Ave",
              "description": "Integration site"
            }
            """
            .formatted(unique);

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/headquarters", createJson)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value(unique))
            .andExpect(jsonPath("$.address").value("1200 Industrial Ave"))
            .andExpect(jsonPath("$.description").value("Integration site"))
            .andReturn();

    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.name").value(unique));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/headquarters/name/" + java.net.URLEncoder.encode(unique, java.nio.charset.StandardCharsets.UTF_8),
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray());

    String updateJson =
        """
            {
              "name": "%s",
              "address": "Updated address",
              "description": "Updated notes"
            }
            """
            .formatted(unique);

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer("/api/v1/headquarters/" + id, token, updateJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.address").value("Updated address"))
        .andExpect(jsonPath("$.description").value("Updated notes"));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/export?page=0&size=50", token))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("sedes_reporte.xlsx")))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_TYPE,
                    containsString(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/statistics", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").exists())
        .andExpect(jsonPath("$.active").exists())
        .andExpect(jsonPath("$.softDeleted").exists());
  }

  @Test
  void getById_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("HEADQUARTER_NOT_FOUND"));
  }

  @Test
  void getById_invalidPath_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/not-numeric", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
  }

  @Test
  void getByName_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/headquarters/name/"
                    + java.net.URLEncoder.encode(
                        "missing-name-" + UUID.randomUUID(), java.nio.charset.StandardCharsets.UTF_8),
                token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("HEADQUARTER_NOT_FOUND"));
  }

  @Test
  void softDelete_getByIdStillReturnsRecord_getByNameReturns404() throws Exception {
    String token = obtainAccessToken();
    String name = "IT-DEL-" + UUID.randomUUID();
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/headquarters", createBody(name, "a", "b"))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/headquarters/" + id, token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deletedAt").exists());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/headquarters/name/"
                    + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8),
                token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("HEADQUARTER_NOT_FOUND"));
  }

  @Test
  void importHeadquarters_emptyFile_returns400() throws Exception {
    String token = obtainAccessToken();
    MockMultipartFile file =
        new MockMultipartFile("file", "empty.xlsx", "application/octet-stream", new byte[0]);

    mockMvc
        .perform(
            multipart("/api/v1/headquarters/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void importHeadquarters_dryRun_doesNotPersist() throws Exception {
    String token = obtainAccessToken();
    String name = "IT-HQ-DRY-" + UUID.randomUUID();
    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "hq.xlsx",
            new String[] {"ID", "Name", "Address", "Description"},
            new Object[] {null, name, "Calle 1", "Nueva sede"});

    mockMvc
        .perform(
            multipart("/api/v1/headquarters/import")
                .file(file)
                .param("dryRun", "true")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dryRun").value(true))
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/headquarters/name/"
                    + java.net.URLEncoder.encode(name, java.nio.charset.StandardCharsets.UTF_8),
                token))
        .andExpect(status().isNotFound());
  }

  @Test
  void importHeadquarters_oneInvalidRow_writesNothing() throws Exception {
    String token = obtainAccessToken();
    String validName = "IT-HQ-OK-" + UUID.randomUUID();
    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "hq.xlsx",
            new String[] {"ID", "Name", "Address", "Description"},
            new Object[] {null, validName, "Calle 2", "Ok"},
            new Object[] {null, "IT-HQ-BAD-" + UUID.randomUUID(), "", "sin dirección"});

    mockMvc
        .perform(
            multipart("/api/v1/headquarters/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dryRun").value(false))
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.updated").value(0))
        .andExpect(jsonPath("$.errors", hasSize(1)));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/headquarters/name/"
                    + java.net.URLEncoder.encode(validName, java.nio.charset.StandardCharsets.UTF_8),
                token))
        .andExpect(status().isNotFound());
  }

  @Test
  void importHeadquarters_blankAddressOnUpdate_keepsExisting() throws Exception {
    String token = obtainAccessToken();
    String name = "IT-HQ-PATCH-" + UUID.randomUUID();
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/headquarters", createBody(name, "Original Ave", "Desc orig"))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");
    String newName = name + "-renamed";

    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "hq.xlsx",
            new String[] {"ID", "Name", "Address", "Description"},
            new Object[] {id, newName, "", ""});

    mockMvc
        .perform(
            multipart("/api/v1/headquarters/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value(newName))
        .andExpect(jsonPath("$.address").value("Original Ave"))
        .andExpect(jsonPath("$.description").value("Desc orig"));
  }

  private static String createBody(String name, String address, String description) {
    try {
      return new com.fasterxml.jackson.databind.ObjectMapper()
          .createObjectNode()
          .put("name", name)
          .put("address", address == null ? "" : address)
          .put("description", description == null ? "" : description)
          .toString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static long extractLongId(String json, String path) {
    Number n = JsonPath.read(json, path);
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-hq-" + UUID.randomUUID() + "@mail.com";
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
