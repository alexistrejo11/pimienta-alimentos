package io.github.alexistrejo11.pimienta.module.notification.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationChannel;
import io.github.alexistrejo11.pimienta.module.notification.core.domain.enums.NotificationStatus;
import io.github.alexistrejo11.pimienta.module.notification.infrastructure.adapter.out.persistence.NotificationJpaRepository;
import io.github.alexistrejo11.pimienta.module.notification.integration.support.NotificationTestData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class NotificationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Autowired private NotificationJpaRepository notificationJpaRepository;

  @Test
  void managementEndpoints_withoutToken_return401() throws Exception {
    mockMvc.perform(get("/api/v1/notifications/management")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/v1/notifications/management/statistics"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void managementSearch_activeUserWithoutAdmin_returns403() throws Exception {
    String token = obtainAccessToken(Set.of());
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/notifications/management?page=0&size=10", token))
        .andExpect(status().isForbidden());
  }

  @Test
  void managementStatistics_missingInterval_returns400() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/notifications/management/statistics", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void managementSearch_andStatistics_withSeededData_return200() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String correlationId = "it-admin-" + suffix;
    LocalDateTime now = LocalDateTime.now();

    UUID sentId =
        NotificationTestData.seed(
            notificationJpaRepository,
            NotificationChannel.EMAIL,
            NotificationStatus.SENT,
            correlationId,
            "IT subject " + suffix,
            now);
    NotificationTestData.seed(
        notificationJpaRepository,
        NotificationChannel.EMAIL,
        NotificationStatus.FAILED,
        correlationId + "-failed",
        "IT failed " + suffix,
        now);

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/notifications/management?page=0&size=20&channel=EMAIL&correlationId="
                    + correlationId,
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items[0].id").value(sentId.toString()))
        .andExpect(jsonPath("$.items[0].channel").value("EMAIL"))
        .andExpect(jsonPath("$.items[0].status").value("SENT"))
        .andExpect(jsonPath("$.metadata.pageNumber").value(0));

    LocalDateTime from = now.minusHours(1);
    LocalDateTime to = now.plusHours(1);
    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/notifications/management/statistics?from="
                    + from
                    + "&to="
                    + to,
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.sent").exists())
        .andExpect(jsonPath("$.failed").exists())
        .andExpect(jsonPath("$.countByChannel.EMAIL").exists());
  }

  @Test
  void logsEndpoints_withoutToken_return401() throws Exception {
    mockMvc.perform(get("/api/v1/notifications/logs")).andExpect(status().isUnauthorized());
  }

  @Test
  void logsSearch_userRole_returns403() throws Exception {
    String token = obtainAccessToken(Set.of());
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/notifications/logs?page=0&size=10", token))
        .andExpect(status().isForbidden());
  }

  @Test
  void logsSearch_manager_returns403() throws Exception {
    String token = obtainAccessToken(Set.of(Role.MANAGER));
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/notifications/logs?page=0&size=10", token))
        .andExpect(status().isForbidden());
  }

  @Test
  void logsSearch_admin_seesTodayLogChannelOnly() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    String correlationId = "it-log-" + suffix;
    LocalDateTime today = LocalDateTime.now();

    UUID logId =
        NotificationTestData.seed(
            notificationJpaRepository,
            NotificationChannel.LOG,
            NotificationStatus.SENT,
            correlationId,
            "IT log " + suffix,
            today);

    NotificationTestData.seed(
        notificationJpaRepository,
        NotificationChannel.EMAIL,
        NotificationStatus.SENT,
        correlationId + "-email",
        "IT email should not appear in logs",
        today);

    LocalDateTime yesterday = LocalDate.now().minusDays(1).atTime(LocalTime.NOON);
    NotificationTestData.seed(
        notificationJpaRepository,
        NotificationChannel.LOG,
        NotificationStatus.SENT,
        correlationId + "-yesterday",
        "IT log yesterday",
        yesterday);

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/notifications/logs?page=0&size=20&correlationId=" + correlationId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(logId.toString()))
        .andExpect(jsonPath("$.items[0].channel").value("LOG"));
  }

  @Test
  void logsSearch_adminRole_returns200() throws Exception {
    String token = obtainAccessToken(Set.of(Role.ADMIN));
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/notifications/logs?page=0&size=5", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata").exists());
  }

  private String obtainAccessToken(Set<Role> roles) throws Exception {
    String email = "it-notif-" + UUID.randomUUID() + "@mail.com";
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
