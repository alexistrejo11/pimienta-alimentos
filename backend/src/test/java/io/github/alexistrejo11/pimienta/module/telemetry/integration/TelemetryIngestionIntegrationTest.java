package io.github.alexistrejo11.pimienta.module.telemetry.integration;

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
class TelemetryIngestionIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void webEvent_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/telemetry/web/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(webEventJson()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void webEvent_managerStaffJwt_isAccepted() throws Exception {
    String managerToken = obtainAccessToken(Role.MANAGER);
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/telemetry/web/events", managerToken, webEventJson()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accepted").value(1));
  }

  @Test
  void webEvent_invalidPayload_returns400() throws Exception {
    String token = obtainAccessToken(Role.ADMIN);
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/telemetry/web/events",
                token,
                """
                {"schemaVersion":1,"eventType":"x","level":"DEBUG","message":"nope","occurredAt":"2026-01-01T00:00:00Z"}
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void webEvent_deviceJwt_returns403() throws Exception {
    String deviceToken = enrollDeviceAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/telemetry/web/events", deviceToken, webEventJson()))
        .andExpect(status().isForbidden());
  }

  @Test
  void posBatch_deviceJwt_isAccepted() throws Exception {
    String deviceToken = enrollDeviceAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/pos/telemetry/events",
                deviceToken,
                """
                {
                  "events": [
                    {
                      "schemaVersion": 1,
                      "eventType": "sync_failure",
                      "level": "ERROR",
                      "message": "retrying",
                      "occurredAt": "2026-01-01T00:00:00Z"
                    }
                  ],
                  "health": {
                    "syncState": "RETRYING",
                    "pendingEvents": 2,
                    "oldestPendingAgeSeconds": 30,
                    "appVersion": "2.1.0"
                  }
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accepted").value(1));
  }

  private static String webEventJson() {
    return """
        {
          "schemaVersion": 1,
          "eventType": "unhandled_exception",
          "level": "ERROR",
          "message": "boom",
          "route": "/app",
          "occurredAt": "2026-01-01T00:00:00Z"
        }
        """;
  }

  private String enrollDeviceAccessToken() throws Exception {
    String staffToken = obtainAccessToken(Role.ADMIN);
    long hqId = createHeadquarter(staffToken, "TEL-HQ-" + UUID.randomUUID());
    MvcResult codeResult =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/enrollment-codes",
                    staffToken,
                    "{\"headquarterId\": %d}".formatted(hqId)))
            .andExpect(status().isOk())
            .andReturn();
    String code = JsonPath.read(codeResult.getResponse().getContentAsString(), "$.code");
    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll",
                    """
                    {
                      "enrollmentCode": "%s",
                      "devicePublicId": "%s",
                      "deviceName": "Telemetry IT",
                      "appVersion": "1.0.0"
                    }
                    """
                        .formatted(code, UUID.randomUUID())))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");
  }

  private long createHeadquarter(String token, String name) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/headquarters",
                    token,
                    """
                    {"name":"%s","address":"Addr","description":"d"}
                    """
                        .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
    Number n = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    return n.longValue();
  }

  private String obtainAccessToken(Role role) throws Exception {
    String email = "it-tel-" + UUID.randomUUID() + "@mail.com";
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
    u.setRoles(new LinkedHashSet<>(Set.of(role)));
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
