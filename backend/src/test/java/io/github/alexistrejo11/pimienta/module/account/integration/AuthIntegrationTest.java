package io.github.alexistrejo11.pimienta.module.account.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Test
  void register_happyPath_createsPendingUserAndReturnsCreated() throws Exception {
    String email = "it-register-" + UUID.randomUUID() + "@mail.com";
    String rawPassword = "StrongPass123!";
    String phone = uniquePhoneE164();
    String requestBody =
        AccountTestRequests.registerJson(
            "Alexis", "Trejo", email, phone, rawPassword, "MALE", LocalDate.of(2000, 1, 10));

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Registro exitoso. Tu cuenta está pendiente de aprobación por un administrador."))
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

    UserJpaEntity saved =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new AssertionError("User was not persisted"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getEmail()).isEqualTo(email);
    assertThat(saved.getFirstName()).isEqualTo("Alexis");
    assertThat(saved.getLastName()).isEqualTo("Trejo");
    assertThat(saved.getPhone()).isEqualTo(phone);
    assertThat(saved.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 10));
    assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
    assertThat(saved.getRoles()).isEmpty();
    assertThat(saved.getPasswordHash()).isNotEqualTo(rawPassword);
    assertThat(passwordEncoder.matches(rawPassword, saved.getPasswordHash())).isTrue();
  }

  @Test
  void register_validation_blankFirstName_returns400WithFieldError() throws Exception {
    String email = "it-badfn-" + UUID.randomUUID() + "@mail.com";
    String body =
        AccountTestRequests.registerJson(
            " ", "Trejo", email, uniquePhoneE164(), "StrongPass123!", "MALE", LocalDate.of(2000, 1, 10));

    mockMvc
        .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value("La validación de la solicitud falló."))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("firstName")));
  }

  @Test
  void register_validation_invalidEmailFormat_returns400() throws Exception {
    String body =
        AccountTestRequests.registerJson(
            "A",
            "B",
            "not-an-email",
            uniquePhoneE164(),
            "StrongPass123!",
            "MALE",
            LocalDate.of(2000, 1, 10));

    mockMvc
        .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void register_validation_weakPassword_returns400() throws Exception {
    String email = "it-weak-" + UUID.randomUUID() + "@mail.com";
    String body =
        AccountTestRequests.registerJson(
            "A", "B", email, uniquePhoneE164(), "short1", "MALE", LocalDate.of(2000, 1, 10));

    mockMvc
        .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void register_validation_underMinimumAge_returns400() throws Exception {
    String email = "it-young-" + UUID.randomUUID() + "@mail.com";
    LocalDate dob = LocalDate.now().minusYears(10);
    String body =
        AccountTestRequests.registerJson(
            "A", "B", email, uniquePhoneE164(), "StrongPass123!", "FEMALE", dob);

    mockMvc
        .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void register_duplicateEmail_returns409() throws Exception {
    String email = "it-dup-" + UUID.randomUUID() + "@mail.com";
    String p1 = uniquePhoneE164();
    String p2 = uniquePhoneE164();
    String b1 = AccountTestRequests.validRegisterJson(email, p1, "StrongPass123!");
    mockMvc
        .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(b1))
        .andExpect(status().isCreated());

    String b2 = AccountTestRequests.registerJson("Other", "User", email, p2, "StrongPass9!", "MALE", LocalDate.of(2000, 1, 10));
    mockMvc
        .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(b2))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.message").value("El correo electrónico ya está registrado."));
  }

  @Test
  void login_happyPath_afterApproval_returnsTokens() throws Exception {
    String email = "it-login-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    String password = "GoodPass1!";
    registerUser(email, phone, password);
    activateUser(email);

    MvcResult res =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.accessToken").isString())
            .andExpect(jsonPath("$.refreshToken").isString())
            .andExpect(jsonPath("$.expiresInSeconds").isNumber())
            .andReturn();

    String body = res.getResponse().getContentAsString();
    String access = JsonPath.read(body, "$.accessToken");
    assertThat(access).isNotBlank();
  }

  @Test
  void login_pendingApproval_returns403() throws Exception {
    String email = "it-pend-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    registerUser(email, phone, "StrongPass1!");

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login", AccountTestRequests.loginJson(email, "StrongPass1!")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("ACCOUNT_PENDING_APPROVAL"))
        .andExpect(
            jsonPath("$.message")
                .value(
                    "Tu cuenta está pendiente de aprobación. Espera a que un administrador la active."));
  }

  @Test
  void login_wrongPassword_returns401() throws Exception {
    String email = "it-badpw-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    registerUser(email, phone, "RightPass1!");
    activateUser(email);

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login", AccountTestRequests.loginJson(email, "WrongPass1!")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
  }

  @Test
  void login_validation_blankEmail_returns400() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login", "{\"email\":\"  \",\"password\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void login_validation_invalidJson_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void login_unknownUser_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login",
                AccountTestRequests.loginJson("missing-" + UUID.randomUUID() + "@mail.com", "AnyPass1!")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_FAILED"));
  }

  @Test
  void refresh_happyPath_returnsNewAccessTokenAndKeepsRefreshToken() throws Exception {
    String email = "it-ref-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    String password = "StrongP1x!";
    registerUser(email, phone, password);
    activateUser(email);

    MvcResult loginRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    String loginBody = loginRes.getResponse().getContentAsString();
    String refresh = JsonPath.read(loginBody, "$.refreshToken");

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/refresh", AccountTestRequests.refreshJson(refresh)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isString())
        .andExpect(jsonPath("$.refreshToken").value(refresh));
  }

  @Test
  void refresh_canReuseSameRefreshToken() throws Exception {
    String email = "it-ref2-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    String password = "StrongP1x!";
    registerUser(email, phone, password);
    activateUser(email);

    MvcResult loginRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    String refresh = JsonPath.read(loginRes.getResponse().getContentAsString(), "$.refreshToken");

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/refresh", AccountTestRequests.refreshJson(refresh)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refreshToken").value(refresh));

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/refresh", AccountTestRequests.refreshJson(refresh)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refreshToken").value(refresh));
  }

  @Test
  void refresh_validation_missingRefreshToken_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void refresh_invalidToken_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/refresh", AccountTestRequests.refreshJson("not-a-valid-jwt")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logout_happyPath_returns204() throws Exception {
    String email = "it-out-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    String password = "StrongP1y!";
    registerUser(email, phone, password);
    activateUser(email);

    MvcResult loginRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    String refresh = JsonPath.read(loginRes.getResponse().getContentAsString(), "$.refreshToken");

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(AccountTestRequests.logoutJson(refresh)))
        .andExpect(status().isNoContent());
  }

  @Test
  void logout_revokesRefreshToken() throws Exception {
    String email = "it-out-rev-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    String password = "StrongP1y!";
    registerUser(email, phone, password);
    activateUser(email);

    MvcResult loginRes =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    String refresh = JsonPath.read(loginRes.getResponse().getContentAsString(), "$.refreshToken");

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(AccountTestRequests.logoutJson(refresh)))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/refresh", AccountTestRequests.refreshJson(refresh)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void logout_noBody_returns204() throws Exception {
    mockMvc
        .perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  private void registerUser(String email, String phone, String password) throws Exception {
    String body = AccountTestRequests.validRegisterJson(email, phone, password);
    mockMvc
        .perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated());
  }

  private void activateUser(String email) {
    UserJpaEntity u =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new AssertionError("user missing: " + email));
    u.setAccountStatus(AccountStatus.ACTIVE);
    userJpaRepository.saveAndFlush(u);
  }

  private static String uniquePhoneE164() {
    long n = Math.abs(ThreadLocalRandom.current().nextLong());
    return "+52" + String.format("%010d", n % 10_000_000_000L);
  }
}
