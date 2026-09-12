package io.github.alexistrejo11.pimienta.module.employees.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import java.time.LocalDate;
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
class AttendanceIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @Test
  void startWorkday_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/employees/1/attendance/start-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"headquarterId\":1}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getAttendance_byId_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/employees/attendances/1")).andExpect(status().isUnauthorized());
  }

  @Test
  void startWorkday_validation_missingHeadquarterId_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/employees/1/attendance/start-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void startWorkday_malformedJson_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/employees/1/attendance/start-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void getAttendance_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/employees/attendances/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("ATTENDANCE_NOT_FOUND"));
  }

  @Test
  void startWorkday_unknownEmployee_returns404() throws Exception {
    String token = obtainAccessToken();
    long hqId = createHeadquarter(token, "HQ-ATT-" + UUID.randomUUID());
    String body = "{\"headquarterId\": " + hqId + "}";
    mockMvc
        .perform(
            post("/api/v1/employees/999999998/attendance/start-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_NOT_FOUND"));
  }

  @Test
  void startWorkday_secondOpenSameDay_returns409() throws Exception {
    String token = obtainAccessToken();
    long hqId = createHeadquarter(token, "HQ-DUP-" + UUID.randomUUID());
    String email = "att-dup-" + UUID.randomUUID() + "@mail.com";
    long employeeId = createEmployee(token, email, "EMP-DUP-" + uuidSuffix());

    String startBody = "{\"headquarterId\": " + hqId + "}";
    mockMvc
        .perform(
            post("/api/v1/employees/" + employeeId + "/attendance/start-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(startBody))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/api/v1/employees/" + employeeId + "/attendance/start-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(startBody))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("ATTENDANCE_ALREADY_OPEN"));
  }

  @Test
  void listAttendancesForToday_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/employees/attendances/for-today?page=0&size=10")).andExpect(status().isUnauthorized());
  }

  @Test
  void searchAttendances_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/employees/attendances/search?page=0&size=10"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listAttendancesByEmployee_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/employees/1/attendances?page=0&size=10")).andExpect(status().isUnauthorized());
  }

  @Test
  void endWorkday_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/employees/1/attendance/end-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listAttendancesByEmployee_unknownEmployee_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/999999997/attendances?page=0&size=10", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_NOT_FOUND"));
  }

  @Test
  void listAttendancesByEmployee_dateRangeInverted_returns400() throws Exception {
    String token = obtainAccessToken();
    String email = "att-range-" + UUID.randomUUID() + "@mail.com";
    long employeeId = createEmployee(token, email, "EMP-RNG-" + uuidSuffix());
    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/"
                    + employeeId
                    + "/attendances?workDateFrom=2026-05-10&workDateTo=2026-05-01&page=0&size=10",
                token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
  }

  @Test
  void listAttendancesByEmployee_withDateRange_returnsTodayRow() throws Exception {
    String token = obtainAccessToken();
    long hqId = createHeadquarter(token, "HQ-RANGE-OK-" + UUID.randomUUID());
    String email = "att-range-ok-" + UUID.randomUUID() + "@mail.com";
    long employeeId = createEmployee(token, email, "EMP-RNGOK-" + uuidSuffix());
    LocalDate today = LocalDate.now();
    String startBody =
        """
            {
              "headquarterId": %d,
              "workDate": "%s"
            }
            """
            .formatted(hqId, today);

    MvcResult started =
        mockMvc
            .perform(
                post("/api/v1/employees/" + employeeId + "/attendance/start-workday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .content(startBody))
            .andExpect(status().isCreated())
            .andReturn();
    long attendanceId = extractLongId(started.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/"
                    + employeeId
                    + "/attendances?workDateFrom="
                    + today
                    + "&workDateTo="
                    + today
                    + "&page=0&size=10",
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(attendanceId))
        .andExpect(jsonPath("$.metadata").exists());
  }

  @Test
  void endWorkday_withoutOpenAttendance_returns409() throws Exception {
    String token = obtainAccessToken();
    String email = "att-end-" + UUID.randomUUID() + "@mail.com";
    long employeeId = createEmployee(token, email, "EMP-END-" + uuidSuffix());

    mockMvc
        .perform(
            post("/api/v1/employees/" + employeeId + "/attendance/end-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorCode").value("ATTENDANCE_NOT_OPEN_FOR_CHECKOUT"));
  }

  @Test
  void attendance_happyPath_startQueriesEnd() throws Exception {
    String token = obtainAccessToken();
    long hqId = createHeadquarter(token, "HQ-HAPPY-" + UUID.randomUUID());
    String email = "att-happy-" + UUID.randomUUID() + "@mail.com";
    long employeeId = createEmployee(token, email, "EMP-HAP-" + uuidSuffix());

    String startBody =
        """
            {
              "headquarterId": %d
            }
            """
            .formatted(hqId);

    MvcResult started =
        mockMvc
            .perform(
                post("/api/v1/employees/" + employeeId + "/attendance/start-workday")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .content(startBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employeeId").value(employeeId))
            .andExpect(jsonPath("$.headquarterId").value(hqId))
            .andExpect(jsonPath("$.status").value("CHECKED_IN"))
            .andExpect(jsonPath("$.checkInEvidencePhotoUrl").value(""))
            .andExpect(jsonPath("$.checkOutTime").value(nullValue()))
            .andReturn();

    long attendanceId = extractLongId(started.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/attendances/" + attendanceId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(attendanceId))
        .andExpect(jsonPath("$.status").value("CHECKED_IN"));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/attendances/for-today?headquarterId="
                    + hqId
                    + "&page=0&size=20",
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(attendanceId))
        .andExpect(jsonPath("$.items[0].employeeId").value(employeeId))
        .andExpect(jsonPath("$.metadata").exists());

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/employees/attendances/for-today?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(greaterThanOrEqualTo(1)));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/" + employeeId + "/attendances?page=0&size=10", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(attendanceId))
        .andExpect(jsonPath("$.metadata").exists());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/employees/attendances/search?employeeId="
                    + employeeId
                    + "&page=0&size=10",
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(attendanceId))
        .andExpect(jsonPath("$.metadata").exists());

    String endBody = "{}";
    mockMvc
        .perform(
            post("/api/v1/employees/" + employeeId + "/attendance/end-workday")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content(endBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(attendanceId))
        .andExpect(jsonPath("$.status").value("CHECKED_OUT"))
        .andExpect(jsonPath("$.checkOutEvidencePhotoUrl").value(""))
        .andExpect(jsonPath("$.minutesWorked", greaterThanOrEqualTo(0)));
  }

  private static String uuidSuffix() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  private static long extractLongId(String json, String jsonPath) {
    Number n = JsonPath.read(json, jsonPath);
    return n.longValue();
  }

  private long createHeadquarter(String token, String uniqueName) throws Exception {
    String json =
        """
            {
              "name": "%s",
              "address": "1 Test Way",
              "description": "IT fixture"
            }
            """
            .formatted(uniqueName);
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/headquarters", json)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    return extractLongId(r.getResponse().getContentAsString(), "$.id");
  }

  private long createEmployee(String token, String email, String employeeNumber) throws Exception {
    String body =
        """
            {
              "firstName": "Att",
              "lastName": "Tester",
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
              "onboardingPhase": "DRAFT"
            }
            """
            .formatted(email, employeeNumber);
    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/employees", body)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    return extractLongId(r.getResponse().getContentAsString(), "$.id");
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-attendance-" + UUID.randomUUID() + "@mail.com";
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
