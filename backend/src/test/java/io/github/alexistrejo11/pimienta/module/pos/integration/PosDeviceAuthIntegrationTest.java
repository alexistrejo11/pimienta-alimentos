package io.github.alexistrejo11.pimienta.module.pos.integration;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.entity.PosEnrollmentCodeJpaEntity;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosEnrollmentCodeSpringDataRepository;
import java.time.LocalDateTime;
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
class PosDeviceAuthIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private PosEnrollmentCodeSpringDataRepository enrollmentCodeJpa;

  @Test
  void enroll_refresh_me_happyPath() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-B2-" + UUID.randomUUID());
    String code = createEnrollmentCode(staffToken, hqId);
    UUID deviceId = UUID.randomUUID();

    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll", enrollJson(code, deviceId, "Caja 1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
            .andExpect(jsonPath("$.status").value("AUTHORIZED"))
            .andExpect(jsonPath("$.visibleCode").value("T1"))
            .andExpect(jsonPath("$.site.id").value(String.valueOf(hqId)))
            .andExpect(jsonPath("$.accessToken", not(nullValue())))
            .andExpect(jsonPath("$.refreshToken", not(nullValue())))
            .andReturn();

    String access = JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");
    String refresh = JsonPath.read(enroll.getResponse().getContentAsString(), "$.refreshToken");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/devices/me", access))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deviceId").value(deviceId.toString()))
        .andExpect(jsonPath("$.status").value("AUTHORIZED"));

    MvcResult refreshed =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/refresh",
                    "{\"refreshToken\": \"%s\"}".formatted(refresh)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", not(nullValue())))
            .andExpect(jsonPath("$.refreshToken", not(nullValue())))
            .andReturn();

    String newRefresh =
        JsonPath.read(refreshed.getResponse().getContentAsString(), "$.refreshToken");
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/pos/devices/refresh",
                "{\"refreshToken\": \"%s\"}".formatted(refresh)))
        .andExpect(status().isUnauthorized());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/pos/devices/refresh",
                "{\"refreshToken\": \"%s\"}".formatted(newRefresh)))
        .andExpect(status().isOk());
  }

  @Test
  void enroll_reusedCode_returns409() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-REUSE-" + UUID.randomUUID());
    String code = createEnrollmentCode(staffToken, hqId);

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/pos/devices/enroll", enrollJson(code, UUID.randomUUID(), "A")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/pos/devices/enroll", enrollJson(code, UUID.randomUUID(), "B")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("POS_ENROLLMENT_CODE_CONSUMED"));
  }

  @Test
  void enroll_expiredCode_returns400() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-EXP-" + UUID.randomUUID());
    String code = createEnrollmentCode(staffToken, hqId);

    PosEnrollmentCodeJpaEntity entity =
        enrollmentCodeJpa.findByCodeAndDeletedAtIsNull(code).orElseThrow();
    entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
    enrollmentCodeJpa.saveAndFlush(entity);

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/pos/devices/enroll", enrollJson(code, UUID.randomUUID(), "Late")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("POS_ENROLLMENT_CODE_EXPIRED"));
  }

  @Test
  void revoke_blocksMeAndRefresh() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-REV-" + UUID.randomUUID());
    String code = createEnrollmentCode(staffToken, hqId);
    UUID deviceId = UUID.randomUUID();

    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll", enrollJson(code, deviceId, "RevokeMe")))
            .andExpect(status().isOk())
            .andReturn();
    String access = JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");
    String refresh = JsonPath.read(enroll.getResponse().getContentAsString(), "$.refreshToken");

    mockMvc
        .perform(
            AccountTestRequests.postBearer(
                "/api/v1/pos/admin/devices/" + deviceId + "/revoke", staffToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REVOKED"));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/devices/me", access))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.errorCode").value("POS_DEVICE_REVOKED"));

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/pos/devices/refresh",
                "{\"refreshToken\": \"%s\"}".formatted(refresh)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void staffToken_cannotCallDeviceMe() throws Exception {
    String staffToken = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/devices/me", staffToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void deviceToken_cannotCallAdmin() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-SEP-" + UUID.randomUUID());
    String code = createEnrollmentCode(staffToken, hqId);
    MvcResult enroll =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll",
                    enrollJson(code, UUID.randomUUID(), "Sep")))
            .andExpect(status().isOk())
            .andReturn();
    String deviceAccess =
        JsonPath.read(enroll.getResponse().getContentAsString(), "$.accessToken");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/admin/operators", deviceAccess))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/headquarters", deviceAccess))
        .andExpect(status().isForbidden());
  }

  @Test
  void operators_crud_smoke() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-OP-" + UUID.randomUUID());

    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/operators",
                    staffToken,
                    """
                    {
                      "displayName": "Cajera IT",
                      "posRole": "CASHIER",
                      "pin": "1234",
                      "headquarterIds": [%d]
                    }
                    """
                        .formatted(hqId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.displayName").value("Cajera IT"))
            .andExpect(jsonPath("$.posRole").value("CASHIER"))
            .andExpect(jsonPath("$.headquarterIds[0]").value(hqId))
            .andReturn();

    long opId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/pos/admin/operators/" + opId, staffToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(opId));

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/pos/admin/operators?page=0&size=20", staffToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());
  }

  private String createEnrollmentCode(String staffToken, long hqId) throws Exception {
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/enrollment-codes",
                    staffToken,
                    "{\"headquarterId\": %d}".formatted(hqId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code", not(nullValue())))
            .andReturn();
    return JsonPath.read(r.getResponse().getContentAsString(), "$.code");
  }

  private static String enrollJson(String code, UUID deviceId, String name) {
    return """
        {
          "enrollmentCode": "%s",
          "devicePublicId": "%s",
          "deviceName": "%s",
          "appVersion": "1.0.0"
        }
        """
        .formatted(code, deviceId, name);
  }

  private long createHeadquarter(String token, String name) throws Exception {
    String body =
        """
        {"name": "%s", "address": "Addr", "description": "d"}
        """
            .formatted(name);
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/headquarters", body)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    Number n = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-pos-b2-" + UUID.randomUUID() + "@mail.com";
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
