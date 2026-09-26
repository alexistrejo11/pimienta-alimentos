package io.github.alexistrejo11.pimienta.module.account.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
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
class UserProfileIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @Test
  void getMe_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void getMe_withInvalidBearer_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/users/me", "definitely.not.a.jwt"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void fullFlow_loginThenMeAndPatch() throws Exception {
    String email = "it-user-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    String password = "Str0ngPass!";
    String registerBody = AccountTestRequests.validRegisterJson(email, phone, password);

    mockMvc
        .perform(
            post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
        .andExpect(status().isCreated());

    activateUser(email);

    MvcResult loginResult =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();

    String responseJson = loginResult.getResponse().getContentAsString();
    String accessToken = JsonPath.read(responseJson, "$.accessToken");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/users/me", accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.firstName").value("Alexis"))
        .andExpect(jsonPath("$.lastName").value("Trejo"))
        .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));

    String newPhone = uniquePhoneE164();
    String patch =
        AccountTestRequests.updateProfileJson("Alexis", "Patched", newPhone, "MALE");

    mockMvc
        .perform(AccountTestRequests.patchJsonBearer("/api/v1/users/me", accessToken, patch))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lastName").value("Patched"))
        .andExpect(jsonPath("$.phone").value(newPhone));
  }

  @Test
  void patchMe_withToken_validationBadPhone_returns400() throws Exception {
    String email = "it-badph-" + UUID.randomUUID() + "@mail.com";
    String phone = uniquePhoneE164();
    String password = "Str0ngPass!";

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(AccountTestRequests.validRegisterJson(email, phone, password)))
        .andExpect(status().isCreated());
    activateUser(email);

    String access = loginAccessToken(email, password);

    String badPatch =
        """
            {
              "firstName": "A",
              "lastName": "B",
              "gender": "MALE",
              "phone": "123"
            }
            """;

    mockMvc
        .perform(AccountTestRequests.patchJsonBearer("/api/v1/users/me", access, badPatch))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  private void activateUser(String email) {
    UserJpaEntity u =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new AssertionError("user missing: " + email));
    u.setAccountStatus(AccountStatus.ACTIVE);
    userJpaRepository.saveAndFlush(u);
  }

  private String loginAccessToken(String email, String password) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
  }

  private static String uniquePhoneE164() {
    long n = Math.abs(ThreadLocalRandom.current().nextLong());
    return "+52" + String.format("%010d", n % 10_000_000_000L);
  }
}
