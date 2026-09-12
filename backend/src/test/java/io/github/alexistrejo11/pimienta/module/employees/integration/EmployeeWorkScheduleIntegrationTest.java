package io.github.alexistrejo11.pimienta.module.employees.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class EmployeeWorkScheduleIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void workSchedule_get_withoutBearer_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/employees/1/work-schedule"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void workSchedule_put_withoutBearer_returns401() throws Exception {
    mockMvc
        .perform(
            put("/api/v1/employees/1/work-schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleBody("MONDAY", "09:00:00", "18:00:00")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void workSchedule_fullFlow_register_put_get_persistsSlots() throws Exception {
    String token = obtainAccessToken();
    String email = "sched-" + UUID.randomUUID() + "@mail.com";
    String empNo = "EMP-SCH-" + uuidSuffix();

    MvcResult reg =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/employees", minimalRegisterJson(email, empNo))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long employeeId = extractLongId(reg.getResponse().getContentAsString(), "$.id");

    String scheduleJson =
        """
            {
              "slots": [
                { "dayOfWeek": "MONDAY", "startTime": "09:00:00", "endTime": "18:00:00" },
                { "dayOfWeek": "FRIDAY", "startTime": "08:30:00", "endTime": "14:00:00" }
              ]
            }
            """;

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/employees/" + employeeId + "/work-schedule", token, scheduleJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots", hasSize(2)))
        .andExpect(jsonPath("$.slots[0].dayOfWeek").value("MONDAY"))
        .andExpect(jsonPath("$.slots[0].startTime").value("09:00:00"))
        .andExpect(jsonPath("$.slots[0].endTime").value("18:00:00"))
        .andExpect(jsonPath("$.slots[1].dayOfWeek").value("FRIDAY"))
        .andExpect(jsonPath("$.slots[1].startTime").value("08:30:00"))
        .andExpect(jsonPath("$.slots[1].endTime").value("14:00:00"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/" + employeeId + "/work-schedule", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots", hasSize(2)))
        .andExpect(jsonPath("$.slots[0].dayOfWeek").value("MONDAY"))
        .andExpect(jsonPath("$.slots[1].dayOfWeek").value("FRIDAY"));
  }

  @Test
  void workSchedule_afterRegister_get_returnsEmptySlots_untilPut() throws Exception {
    String token = obtainAccessToken();
    String email = "sched-empty-" + UUID.randomUUID() + "@mail.com";
    MvcResult reg =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/employees", minimalRegisterJson(email, "EMP-EMPTY-" + uuidSuffix()))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long employeeId = extractLongId(reg.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/" + employeeId + "/work-schedule", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots").isArray())
        .andExpect(jsonPath("$.slots", hasSize(0)));
  }

  @Test
  void workSchedule_put_startNotBeforeEnd_returns400() throws Exception {
    String token = obtainAccessToken();
    String email = "sched-bad-" + UUID.randomUUID() + "@mail.com";
    MvcResult reg =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/employees", minimalRegisterJson(email, "EMP-BAD-" + uuidSuffix()))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long employeeId = extractLongId(reg.getResponse().getContentAsString(), "$.id");

    String badSchedule =
        """
            {
              "slots": [
                { "dayOfWeek": "WEDNESDAY", "startTime": "18:00:00", "endTime": "09:00:00" }
              ]
            }
            """;

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/employees/" + employeeId + "/work-schedule", token, badSchedule))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
  }

  @Test
  void workSchedule_get_unknownEmployee_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/employees/999999999/work-schedule", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_NOT_FOUND"));
  }

  private static String scheduleBody(String day, String start, String end) {
    return """
        {"slots":[{"dayOfWeek":"%s","startTime":"%s","endTime":"%s"}]}
        """
        .formatted(day, start, end);
  }

  private static String uuidSuffix() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private static String minimalRegisterJson(String email, String employeeNumber) {
    return registerJson("María", "López García", email, employeeNumber, "DRAFT");
  }

  private static String registerJson(
      String firstName,
      String lastName,
      String email,
      String employeeNumber,
      String onboardingPhase) {
    return """
        {
          "firstName": "%s",
          "lastName": "%s",
          "email": "%s",
          "phone": "+52 55 1234 5678",
          "address": "Av. Reforma 123, CDMX",
          "curp": "LOLA850101HDFPLN09",
          "rfc": "XAXX010101000",
          "nss": "12345678901",
          "clabe": "012180001234567890",
          "employeeNumber": "%s",
          "position": "Operador de línea",
          "department": "Producción",
          "contractType": "FIXED_TERM",
          "workShift": "MORNING",
          "salaryPerWeek": 3500.00,
          "birthDate": "1985-01-01",
          "onboardingPhase": "%s"
        }
        """
        .formatted(firstName, lastName, email, employeeNumber, onboardingPhase);
  }

  private static long extractLongId(String json, String jsonPath) {
    Number n = JsonPath.read(json, jsonPath);
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-work-schedule-" + UUID.randomUUID() + "@mail.com";
    String phone =
        "+52"
            + String.format(
                "%010d", Math.abs(ThreadLocalRandom.current().nextLong()) % 10_000_000_000L);
    String password = "Str0ngPass!";
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(AccountTestRequests.validRegisterJson(email, phone, password)))
        .andExpect(status().isCreated());

    UserJpaEntity u =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new AssertionError("user missing"));
    u.setAccountStatus(AccountStatus.ACTIVE);
    u.setRoles(new LinkedHashSet<>(Set.of(Role.ADMIN)));
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
