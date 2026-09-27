package io.github.alexistrejo11.pimienta.module.account.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class UserManagementIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void managementEndpoints_withoutToken_return401() throws Exception {
    mockMvc.perform(get("/api/v1/users/management/statistics")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/v1/users/management")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(post("/api/v1/users/management/1/approve"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getUserById_notFound_returns404() throws Exception {
    String adminToken = obtainAdminToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/management/999999999", adminToken))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
  }

  @Test
  void getUserById_invalidPath_returns400() throws Exception {
    String adminToken = obtainAdminToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/management/not-id", adminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
  }

  @Test
  void getUserByEmail_invalidEmail_returns400() throws Exception {
    String adminToken = obtainAdminToken();
    mockMvc
        .perform(
            get("/api/v1/users/management/by-email")
                .param("email", "not-an-email")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
  }

  @Test
  void replaceRoles_emptyRoles_returns400() throws Exception {
    String adminToken = obtainAdminToken();
    String targetEmail = registerPendingUser();
    long targetId = userIdByEmail(targetEmail);

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/users/management/" + targetId + "/roles",
                adminToken,
                "{\"roles\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void register_pendingApproval_loginBlockedUntilApproved() throws Exception {
    String targetEmail = registerPendingUser();
    String password = "Str0ngPass!";

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login", AccountTestRequests.loginJson(targetEmail, password)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_PENDING_APPROVAL"));

    String adminToken = obtainAdminToken();
    long targetId = userIdByEmail(targetEmail);

    mockMvc
        .perform(AccountTestRequests.postBearer("/api/v1/users/management/" + targetId + "/approve", adminToken))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login", AccountTestRequests.loginJson(targetEmail, password)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString());
  }

  @Test
  void userManagement_fullLifecycle_allPaths() throws Exception {
    String adminToken = obtainAdminToken();

    String targetEmail = "it-target-" + UUID.randomUUID() + "@mail.com";
    String targetPhone = uniquePhoneE164();
    String targetPassword = "TargetPass1!";
    registerUser(targetEmail, targetPhone, targetPassword);
    long targetId = userIdByEmail(targetEmail);

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/management/statistics", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsers").exists())
        .andExpect(jsonPath("$.activeUsers").exists())
        .andExpect(jsonPath("$.bannedUsers").exists());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/management?page=0&size=20", adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata.pageNumber").value(0));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/management/" + targetId, adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(targetId))
        .andExpect(jsonPath("$.email").value(targetEmail))
        .andExpect(jsonPath("$.accountStatus").value("PENDING_APPROVAL"));

    mockMvc
        .perform(
            get("/api/v1/users/management/by-email")
                .param("email", targetEmail)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(targetId));

    mockMvc
        .perform(AccountTestRequests.postBearer("/api/v1/users/management/" + targetId + "/approve", adminToken))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/management/" + targetId, adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

    MvcResult targetLogin =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login",
                    AccountTestRequests.loginJson(targetEmail, targetPassword)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isString())
            .andReturn();
    String targetToken =
        JsonPath.read(targetLogin.getResponse().getContentAsString(), "$.accessToken");

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/users/management/" + targetId + "/roles",
                adminToken,
                 "{\"roles\":[\"SUPPORT\",\"SALES\",\"SALES\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles", hasItem("SUPPORT")))
        .andExpect(jsonPath("$.roles", hasItem("SALES")))
        .andExpect(jsonPath("$.roles", hasSize(2)));

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/users/management/" + targetId + "/roles",
                adminToken,
                "{\"roles\":[\"EMPLOYEE\",\"DIRECTOR\",\"EMPLOYEE\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles", hasItem("EMPLOYEE")))
        .andExpect(jsonPath("$.roles", hasItem("DIRECTOR")))
        .andExpect(jsonPath("$.roles", hasSize(2)))
        .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.not(hasItem("SUPPORT"))));

    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/users/management/" + targetId + "/ban",
                adminToken,
                "{\"reason\":\"IT integration ban\"}"))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/management/" + targetId, adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountStatus").value("BANNED"))
        .andExpect(jsonPath("$.bannedReason").value("IT integration ban"));

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login",
                AccountTestRequests.loginJson(targetEmail, targetPassword)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));

    mockMvc
        .perform(AccountTestRequests.postBearer("/api/v1/users/management/" + targetId + "/unban", adminToken))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/management/" + targetId, adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login",
                AccountTestRequests.loginJson(targetEmail, targetPassword)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString());

    // Target JWT can call an authenticated endpoint (profile) after unban.
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/me", targetToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(targetEmail));
  }

  private String registerPendingUser() throws Exception {
    String email = "it-pending-" + UUID.randomUUID() + "@mail.com";
    registerUser(email, uniquePhoneE164(), "Str0ngPass!");
    return email;
  }

  private void registerUser(String email, String phone, String password) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(AccountTestRequests.validRegisterJson(email, phone, password)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
  }

  private long userIdByEmail(String email) {
    return userJpaRepository
        .findByEmailAndDeletedAtIsNull(email)
        .map(UserJpaEntity::getId)
        .orElseThrow(() -> new AssertionError("user missing: " + email));
  }

  private String obtainAdminToken() throws Exception {
    String email = "it-admin-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    String password = "AdminPass1!";
    registerUser(email, phone, password);

    UserJpaEntity admin =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new AssertionError("admin user missing"));
    admin.setAccountStatus(AccountStatus.ACTIVE);
    admin.setRoles(new LinkedHashSet<>(Set.of(Role.ADMIN)));
    userJpaRepository.saveAndFlush(admin);

    MvcResult login =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
  }

  private static String uniquePhoneE164() {
    long n = Math.abs(ThreadLocalRandom.current().nextLong());
    return "+52" + String.format("%010d", n % 10_000_000_000L);
  }
}
