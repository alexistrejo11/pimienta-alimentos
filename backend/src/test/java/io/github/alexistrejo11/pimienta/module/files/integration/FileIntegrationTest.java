package io.github.alexistrejo11.pimienta.module.files.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import io.github.alexistrejo11.pimienta.module.files.core.port.output.FileStoragePort;
import io.github.alexistrejo11.pimienta.module.files.integration.support.StubFileStoragePort;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
@Import(FileIntegrationTest.StubFileStorageConfig.class)
class FileIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Autowired private FileStoragePort fileStoragePort;

  @TestConfiguration
  static class StubFileStorageConfig {

    @Bean
    @Primary
    FileStoragePort fileStoragePort() {
      return new StubFileStoragePort();
    }
  }

  @Test
  void managementEndpoints_withoutToken_return401() throws Exception {
    mockMvc.perform(get("/api/v1/files/management")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            multipart("/api/v1/files/management/upload")
                .file(tinyFile("empty.txt"))
                .param("category", "TEMPLATE"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void managementUpload_activeUserWithoutAdmin_returns403() throws Exception {
    String token = obtainAccessToken(Set.of());
    mockMvc
        .perform(
            multipart("/api/v1/files/management/upload")
                .file(tinyFile("doc.txt"))
                .param("category", "TEMPLATE")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void managementUpload_emptyFile_returns400Validation() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    mockMvc
        .perform(
            multipart("/api/v1/files/management/upload")
                .file(new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]))
                .param("category", "TEMPLATE")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void managementUpload_getSearchDownloadUrlDelete_flow() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String filename = "it-template-" + suffix + ".txt";

    MvcResult uploaded =
        mockMvc
            .perform(
                multipart("/api/v1/files/management/upload")
                    .file(tinyFile(filename))
                    .param("category", "TEMPLATE")
                    .param("description", "IT upload")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.category").value("TEMPLATE"))
            .andExpect(jsonPath("$.originalName").value(filename))
            .andExpect(jsonPath("$.s3Key").exists())
            .andReturn();

    String assetId = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/files/management/" + assetId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(assetId))
        .andExpect(jsonPath("$.category").value("TEMPLATE"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/files/management?page=0&size=20&category=TEMPLATE&originalNameContains="
                    + suffix,
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata.pageNumber").value(0));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/files/management/" + assetId + "/download-url", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(assetId))
        .andExpect(jsonPath("$.url", startsWith(StubFileStoragePort.PRESIGN_BASE)));

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/files/management/" + assetId, token))
        .andExpect(status().isNoContent());
  }

  @Test
  void managementGetById_notFound_returns500() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    UUID missing = UUID.randomUUID();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/files/management/" + missing, token))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
  }

  @Test
  void managementGetById_invalidUuid_returns400() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/files/management/not-a-uuid", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
  }

  @Test
  void resourcesEndpoints_withoutToken_return401() throws Exception {
    mockMvc.perform(get("/api/v1/files/resources")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            multipart("/api/v1/files/resources/upload")
                .file(tinyFile("r.txt"))
                .param("module", "employees"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void resourcesUpload_userRole_returns403() throws Exception {
    String token = obtainAccessToken(Set.of());
    mockMvc
        .perform(
            multipart("/api/v1/files/resources/upload")
                .file(tinyFile("r.txt"))
                .param("module", "employees")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void resourcesUpload_manager_returns403() throws Exception {
    String token = obtainAccessToken(Set.of(Role.MANAGER));
    mockMvc
        .perform(
            multipart("/api/v1/files/resources/upload")
                .file(tinyFile("r.txt"))
                .param("module", "employees")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  void resourcesUpload_admin_searchAndDownloadUrl_flow() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String filename = "it-resource-" + suffix + ".txt";

    MvcResult uploaded =
        mockMvc
            .perform(
                multipart("/api/v1/files/resources/upload")
                    .file(tinyFile(filename))
                    .param("module", "employees")
                    .param("entityType", "employee")
                    .param("entityId", "42")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.category").value("RESOURCE"))
            .andExpect(jsonPath("$.module").value("employees"))
            .andExpect(jsonPath("$.originalName").value(filename))
            .andReturn();

    String assetId = JsonPath.read(uploaded.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/files/resources?page=0&size=10&module=employees", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/files/resources/" + assetId + "/download-url", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(assetId))
        .andExpect(jsonPath("$.url", containsString("integration.test")));
  }

  @Test
  void fileStoragePort_isStub_notS3Adapter() {
    org.junit.jupiter.api.Assertions.assertInstanceOf(
        StubFileStoragePort.class, fileStoragePort);
  }

  private static MockMultipartFile tinyFile(String filename) {
    return new MockMultipartFile(
        "file",
        filename,
        MediaType.TEXT_PLAIN_VALUE,
        "it".getBytes(StandardCharsets.UTF_8));
  }

  private String obtainAccessToken(Set<Role> roles) throws Exception {
    String email = "it-files-" + UUID.randomUUID() + "@mail.com";
    String phone =
        "+52"
            + String.format(
                "%010d", Math.abs(ThreadLocalRandom.current().nextLong()) % 10_000_000_000L);
    String password = "Str0ngPass!";
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/register",
                AccountTestRequests.validRegisterJson(email, phone, password)))
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
