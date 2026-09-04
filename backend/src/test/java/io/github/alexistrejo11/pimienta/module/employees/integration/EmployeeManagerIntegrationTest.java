package io.github.alexistrejo11.pimienta.module.employees.integration;

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
import java.nio.charset.StandardCharsets;
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
class EmployeeManagerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private UserJpaRepository userJpaRepository;

  @Test
  void statistics_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/employees/statistics")).andExpect(status().isUnauthorized());
  }

  @Test
  void register_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/employees", minimalRegisterJson("x@y.com", "EMP-X")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void register_validation_invalidEmail_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/employees", minimalRegisterJson("not-an-email", "EMP-INV"))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void register_validation_blankName_returns400() throws Exception {
    String token = obtainAccessToken();
    String body =
        registerJson(
            "   ",
            "López",
            "ok-" + UUID.randomUUID() + "@mail.com",
            "EMP-BLANK",
            "DRAFT");
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/employees", body)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void register_validation_nonPositiveSalary_returns400() throws Exception {
    String token = obtainAccessToken();
    String email = "sal-" + UUID.randomUUID() + "@mail.com";
    String body =
        """
            {
              "firstName": "Test",
              "lastName": "User",
              "email": "%s",
              "phone": "+52 55 1000 0001",
              "address": "Calle 1",
              "curp": "LOLA850101HDFPLN09",
              "rfc": "XAXX010101000",
              "nss": "12345678901",
              "clabe": "012180001234567890",
              "employeeNumber": "EMP-ZERO",
              "position": "Operador",
              "department": "Producción",
              "contractType": "FIXED_TERM",
              "workShift": "MORNING",
              "salaryPerWeek": 0,
              "birthDate": "1985-01-01",
              "onboardingPhase": "DRAFT"
            }
            """
            .formatted(email);
    mockMvc
        .perform(
            AccountTestRequests.postJson("/api/v1/employees", body)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void register_validation_malformedJson_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/employees")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void register_multipart_employeeJsonPart_returns201() throws Exception {
    String token = obtainAccessToken();
    String email = "mp-" + UUID.randomUUID() + "@mail.com";
    String empNo = "EMP-MP-" + uuidSuffix();
    String json = minimalRegisterJson(email, empNo);
    MockMultipartFile employee =
        new MockMultipartFile(
            "employee",
            null,
            MediaType.APPLICATION_JSON_VALUE,
            json.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/api/v1/employees")
                .file(employee)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email.toLowerCase()))
        .andExpect(jsonPath("$.employeeNumber").value(empNo));
  }

  @Test
  void register_multipart_employeeOctetStreamPart_returns201() throws Exception {
    String token = obtainAccessToken();
    String email = "mp-os-" + UUID.randomUUID() + "@mail.com";
    String empNo = "EMP-OS-" + uuidSuffix();
    String json = minimalRegisterJson(email, empNo);
    MockMultipartFile employee =
        new MockMultipartFile(
            "employee",
            "blob.bin",
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            json.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/api/v1/employees")
                .file(employee)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(email.toLowerCase()))
        .andExpect(jsonPath("$.employeeNumber").value(empNo));
  }

  @Test
  void register_happyPath_returns201WithId() throws Exception {
    String token = obtainAccessToken();
    String email = "emp-" + UUID.randomUUID() + "@mail.com";
    String empNo = "EMP-IT-" + UUID.randomUUID().toString().substring(0, 8);
    String body = minimalRegisterJson(email, empNo);

    MvcResult r =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/employees", body)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value(email.toLowerCase()))
            .andExpect(jsonPath("$.firstName").value("María"))
            .andExpect(jsonPath("$.lastName").value("López García"))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();

    long id = extractLongId(r.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id));
  }

  @Test
  void getEmployee_notFound_returns404() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/999999999", token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_NOT_FOUND"));
  }

  @Test
  void getEmployee_invalidPathId_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/not-id", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("TYPE_MISMATCH"));
  }

  @Test
  void readEndpoints_withToken_return200() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/statistics", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").exists());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/summary", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalNotDeleted").exists());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees?page=0&size=10", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata").exists());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/active?page=0&size=5", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());
  }

  @Test
  void exportEmployees_withToken_returnsSpreadsheet() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/export?page=0&size=10", token))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("empleados_reporte.xlsx")))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_TYPE,
                    containsString(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
  }

  @Test
  void submitForContract_whenDraft_movesToPendingContract() throws Exception {
    String token = obtainAccessToken();
    String email = "sub-" + UUID.randomUUID() + "@mail.com";
    MvcResult reg =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/employees", minimalRegisterJson(email, "EMP-SUB-" + uuidSuffix()))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andReturn();
    long id = extractLongId(reg.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.putBearer("/api/v1/employees/" + id + "/submit-for-contract", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING_CONTRACT"));

    mockMvc
        .perform(AccountTestRequests.putBearer("/api/v1/employees/" + id + "/submit-for-contract", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void submitForContract_whenRegisteredAsPending_returns400() throws Exception {
    String token = obtainAccessToken();
    String email = "sub2-" + UUID.randomUUID() + "@mail.com";
    String body = registerJson("Pending", "User", email, "EMP-PEND-" + uuidSuffix(), "PENDING_CONTRACT");
    MvcResult reg =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/employees", body)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING_CONTRACT"))
            .andReturn();
    long id = extractLongId(reg.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(AccountTestRequests.putBearer("/api/v1/employees/" + id + "/submit-for-contract", token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void updateTerminateRehireDelete_flow() throws Exception {
    String token = obtainAccessToken();
    String email = "flow-" + UUID.randomUUID() + "@mail.com";
    String empNo = "EMP-FLOW-" + uuidSuffix();
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/employees", minimalRegisterJson(email, empNo))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    String updateBody =
        """
            {
              "firstName": "Updated",
              "lastName": "Worker",
              "email": "%s",
              "phone": "+52 55 2000 0002",
              "address": "Nueva dirección 2",
              "curp": "LOLA850101HDFPLN09",
              "rfc": "XAXX010101000",
              "nss": "12345678901",
              "clabe": "012180001234567890",
              "position": "Supervisor",
              "department": "Calidad",
              "contractType": "INDEFINITE",
              "workShift": "MIXED",
              "salaryPerWeek": 4000.00,
              "bonuses": 100.00,
              "foodVouchers": 50.00,
              "integrationFactor": 1.10
            }
            """
            .formatted("upd-" + UUID.randomUUID() + "@mail.com");

    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer("/api/v1/employees/" + id, token, updateBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Updated"))
        .andExpect(jsonPath("$.lastName").value("Worker"))
        .andExpect(jsonPath("$.department").value("Calidad"));

    mockMvc
        .perform(AccountTestRequests.putBearer("/api/v1/employees/" + id + "/terminate", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("TERMINATED"));

    mockMvc
        .perform(AccountTestRequests.putBearer("/api/v1/employees/" + id + "/rehire", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    mockMvc
        .perform(AccountTestRequests.deleteBearer("/api/v1/employees/" + id, token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/" + id, token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("EMPLOYEE_NOT_FOUND"));
  }

  @Test
  void update_multipart_employeeJsonPart_returns200() throws Exception {
    String token = obtainAccessToken();
    String email = "mp-upd-" + UUID.randomUUID() + "@mail.com";
    String empNo = "EMP-MPU-" + uuidSuffix();
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson("/api/v1/employees", minimalRegisterJson(email, empNo))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    String updEmail = "mp-upd-ch-" + UUID.randomUUID() + "@mail.com";
    String updateJson =
        """
            {
              "firstName": "Multipart",
              "lastName": "Updater",
              "email": "%s",
              "phone": "+52 55 2000 0003",
              "address": "Dir MP",
              "curp": "LOLA850101HDFPLN09",
              "rfc": "XAXX010101000",
              "nss": "12345678901",
              "clabe": "012180001234567890",
              "position": "Lead",
              "department": "Ops",
              "contractType": "INDEFINITE",
              "workShift": "MIXED",
              "salaryPerWeek": 4100.00,
              "bonuses": 110.00,
              "foodVouchers": 55.00,
              "integrationFactor": 1.11
            }
            """
            .formatted(updEmail);

    MockMultipartFile employee =
        new MockMultipartFile(
            "employee",
            null,
            MediaType.APPLICATION_JSON_VALUE,
            updateJson.getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/api/v1/employees/" + id)
                .file(employee)
                .with(
                    request -> {
                      request.setMethod("PUT");
                      return request;
                    })
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Multipart"))
        .andExpect(jsonPath("$.email").value(updEmail.toLowerCase()));
  }

  @Test
  void importEmployees_emptyFile_returns400() throws Exception {
    String token = obtainAccessToken();
    MockMultipartFile file =
        new MockMultipartFile("file", "empty.xlsx", "application/octet-stream", new byte[0]);

    mockMvc
        .perform(
            multipart("/api/v1/employees/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void importEmployees_dryRun_doesNotPersist() throws Exception {
    String token = obtainAccessToken();
    String email = "dry-" + uuidSuffix() + "@mail.com";
    MockMultipartFile file =
        XlsxTestFiles.multipart("emp.xlsx", employeeHeaders(), employeeCreateCells("Ana", "López", email));

    mockMvc
        .perform(
            multipart("/api/v1/employees/import")
                .file(file)
                .param("dryRun", "true")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dryRun").value(true))
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees?page=0&size=100", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.email=='" + email + "')]", hasSize(0)));
  }

  @Test
  void importEmployees_oneInvalidRow_writesNothing() throws Exception {
    String token = obtainAccessToken();
    String okEmail = "ok-" + uuidSuffix() + "@mail.com";
    String badEmail = "bad-" + uuidSuffix() + "@mail.com";
    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "emp.xlsx",
            employeeHeaders(),
            employeeCreateCells("Ana", "López", okEmail),
            employeeCreateCells("Luis", "", badEmail));

    mockMvc
        .perform(
            multipart("/api/v1/employees/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.errors", hasSize(1)));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees?page=0&size=100", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[?(@.email=='" + okEmail + "')]", hasSize(0)));
  }

  @Test
  void importEmployees_blankDepartmentOnUpdate_keepsExisting() throws Exception {
    String token = obtainAccessToken();
    String email = "patch-" + uuidSuffix() + "@mail.com";
    MvcResult created =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/employees",
                        minimalRegisterJson(email, "EMP-" + uuidSuffix()))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long id = extractLongId(created.getResponse().getContentAsString(), "$.id");

    Object[] update = employeeCreateCells("Renamed", "López García", email);
    update[0] = id;
    update[11] = "";

    MockMultipartFile file = XlsxTestFiles.multipart("emp.xlsx", employeeHeaders(), update);

    mockMvc
        .perform(
            multipart("/api/v1/employees/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/employees/" + id, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.firstName").value("Renamed"))
        .andExpect(jsonPath("$.department").value("Producción"));
  }

  private static String[] employeeHeaders() {
    return new String[] {
      "ID",
      "FirstName",
      "LastName",
      "Email",
      "Phone",
      "Address",
      "CURP",
      "RFC",
      "NSS",
      "Clabe",
      "Position",
      "Department",
      "Status",
      "ContractType",
      "WorkShift",
      "SalaryPerWeek",
      "Bonuses",
      "FoodVouchers",
      "IntegrationFactor"
    };
  }

  private static Object[] employeeCreateCells(String firstName, String lastName, String email) {
    return new Object[] {
      null,
      firstName,
      lastName,
      email,
      "+52 55 1234 5678",
      "Av. Reforma 123, CDMX",
      "LOLA850101HDFPLN09",
      "XAXX010101000",
      "12345678901",
      "012180001234567890",
      "Operador de línea",
      "Producción",
      "",
      "FIXED_TERM",
      "MORNING",
      3500,
      null,
      null,
      null
    };
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
    String email = "it-employee-" + UUID.randomUUID() + "@mail.com";
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
