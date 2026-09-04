package io.github.alexistrejo11.pimienta.module.payroll.integration;

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
class PayrollIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserJpaRepository userJpaRepository;

  @Test
  void summary_withoutToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/payroll/summary"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerPeriod_withoutToken_returns401() throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/payroll/periods",
                """
                {"frequency":"WEEKLY","startDate":"2026-04-01","endDate":"2026-04-07"}
                """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerPeriod_malformedJson_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            post("/api/v1/payroll/periods")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .content("{bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("MALFORMED_PAYLOAD"));
  }

  @Test
  void registerPeriod_validation_missingFrequency_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/payroll/periods",
                    """
                    {"startDate":"2026-04-01","endDate":"2026-04-07"}
                    """)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void registerRecord_grossNotPositive_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/payroll/records",
                    """
                    {
                      "employeeId": 900001,
                      "periodId": null,
                      "workedDaysStart": "2026-04-01",
                      "workedDaysEnd": "2026-04-07",
                      "grossAmount": 0
                    }
                    """)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
  }

  @Test
  void registerAdjustment_recordNotFound_returns400() throws Exception {
    String token = obtainAccessToken();
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/payroll/records/999999999/adjustments",
                    """
                    {"type":"BONUS","amount":"50.00","reason":"Test adjustment"}
                    """)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_ARGUMENT"));
  }

  @Test
  void importPayroll_emptyFile_returns400() throws Exception {
    String token = obtainAccessToken();
    MockMultipartFile file =
        new MockMultipartFile("file", "empty.xlsx", "application/octet-stream", new byte[0]);
    mockMvc
        .perform(
            multipart("/api/v1/payroll/import").file(file).header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest());
  }

  @Test
  void importPayroll_dryRun_doesNotPersist() throws Exception {
    String token = obtainAccessToken();
    long employeeId = 8_200_000L + (Math.abs(ThreadLocalRandom.current().nextLong()) % 100_000L);
    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "payroll.xlsx",
            payrollHeaders(),
            new Object[] {
              null, employeeId, null, "2026-06-01", "2026-06-07", 4100, null, null, null, ""
            });

    mockMvc
        .perform(
            multipart("/api/v1/payroll/import")
                .file(file)
                .param("dryRun", "true")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dryRun").value(true))
        .andExpect(jsonPath("$.created").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/payroll/records?page=0&size=20&employeeId=" + employeeId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metadata.totalElements").value(0));
  }

  @Test
  void importPayroll_oneInvalidRow_writesNothing() throws Exception {
    String token = obtainAccessToken();
    long employeeId = 8_200_100L + (Math.abs(ThreadLocalRandom.current().nextLong()) % 100_000L);
    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "payroll.xlsx",
            payrollHeaders(),
            new Object[] {
              null, employeeId, null, "2026-06-01", "2026-06-07", 4100, null, null, null, ""
            },
            new Object[] {
              null, employeeId, null, "2026-06-08", "2026-06-14", null, null, null, null, ""
            });

    mockMvc
        .perform(
            multipart("/api/v1/payroll/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(0))
        .andExpect(jsonPath("$.errors", hasSize(1)));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/payroll/records?page=0&size=20&employeeId=" + employeeId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metadata.totalElements").value(0));
  }

  @Test
  void importPayroll_blankGrossOnUpdate_keepsExisting() throws Exception {
    String token = obtainAccessToken();
    long employeeId = 8_200_200L + (Math.abs(ThreadLocalRandom.current().nextLong()) % 100_000L);
    MvcResult recordResult =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/payroll/records",
                        """
                        {
                          "employeeId": %d,
                          "periodId": null,
                          "workedDaysStart": "2026-06-01",
                          "workedDaysEnd": "2026-06-07",
                          "grossAmount": 5000.00
                        }
                        """
                            .formatted(employeeId))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andReturn();
    long recordId = extractLongId(recordResult.getResponse().getContentAsString(), "$.id");

    MockMultipartFile file =
        XlsxTestFiles.multipart(
            "payroll.xlsx",
            payrollHeaders(),
            new Object[] {
              recordId, employeeId, null, "2026-06-01", "2026-06-15", null, null, null, null, ""
            });

    mockMvc
        .perform(
            multipart("/api/v1/payroll/import")
                .file(file)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.updated").value(1))
        .andExpect(jsonPath("$.errors", hasSize(0)));

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/payroll/records?page=0&size=20&employeeId=" + employeeId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].grossAmount").value(5000.0))
        .andExpect(jsonPath("$.items[0].workedDaysEnd").value("2026-06-15"));
  }

  private static String[] payrollHeaders() {
    return new String[] {
      "id",
      "employee_id",
      "period_id",
      "worked_days_start",
      "worked_days_end",
      "gross_amount",
      "total_discounts",
      "total_bonuses",
      "net_amount",
      "status"
    };
  }

  @Test
  void periodRecordPaymentAdjustmentListsSummaryExport_flow() throws Exception {
    String token = obtainAccessToken();
    long employeeId = 8_000_000L + (Math.abs(ThreadLocalRandom.current().nextLong()) % 100_000L);

    MvcResult periodResult =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/payroll/periods",
                        """
                        {"frequency":"WEEKLY","startDate":"2026-04-01","endDate":"2026-04-07"}
                        """)
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.frequency").value("WEEKLY"))
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andReturn();
    long periodId = extractLongId(periodResult.getResponse().getContentAsString(), "$.id");

    MvcResult recordResult =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                        "/api/v1/payroll/records",
                        """
                        {
                          "employeeId": %d,
                          "periodId": %d,
                          "workedDaysStart": "2026-04-01",
                          "workedDaysEnd": "2026-04-07",
                          "grossAmount": 5000.00
                        }
                        """
                            .formatted(employeeId, periodId))
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.employeeId").value((int) employeeId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andReturn();
    long recordId = extractLongId(recordResult.getResponse().getContentAsString(), "$.id");

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/payroll/records?page=0&size=20&employeeId=" + employeeId + "&periodId=" + periodId,
                token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.metadata.pageNumber").value(0));

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/payroll/periods?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/payroll/payments",
                    """
                    {
                      "employeeId": %d,
                      "frequency": "WEEKLY",
                      "workedDaysStart": "2026-04-01",
                      "workedDaysEnd": "2026-04-07",
                      "grossAmount": 5000.00,
                      "netAmount": 5000.00,
                      "destinationAccount": "ACC-%s",
                      "transactionId": "TX-%s"
                    }
                    """
                        .formatted(employeeId, UUID.randomUUID(), UUID.randomUUID()))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PAID"))
        .andExpect(jsonPath("$.pendingAmount").value(0));

    mockMvc
        .perform(
            AccountTestRequests.getBearer("/api/v1/payroll/payments?page=0&size=20&employeeId=" + employeeId, token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/payroll/records/" + recordId + "/adjustments",
                    """
                    {"type":"BONUS","amount":"100.00","reason":"Integration bonus"}
                    """)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated());

    mockMvc
        .perform(AccountTestRequests.getBearer("/api/v1/payroll/debts?page=0&size=20", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/payroll/summary?from=2026-01-01&to=2026-12-31", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.employeesPaid").exists())
        .andExpect(jsonPath("$.totalGross").exists())
        .andExpect(jsonPath("$.totalNet").exists());

    mockMvc
        .perform(
            AccountTestRequests.getBearer(
                "/api/v1/payroll/export?page=0&size=50&employeeId=" + employeeId, token))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("payroll_reporte.xlsx")))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_TYPE,
                    containsString(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
  }

  @Test
  void registerPayment_partialNet_createsPartialStatus() throws Exception {
    String token = obtainAccessToken();
    long employeeId = 8_100_000L + (Math.abs(ThreadLocalRandom.current().nextLong()) % 100_000L);

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                    "/api/v1/payroll/payments",
                    """
                    {
                      "employeeId": %d,
                      "frequency": "MONTHLY",
                      "workedDaysStart": "2026-05-01",
                      "workedDaysEnd": "2026-05-31",
                      "grossAmount": 4000.00,
                      "netAmount": 3200.00,
                      "destinationAccount": "ACC-PARTIAL",
                      "transactionId": "TX-PART-%s"
                    }
                    """
                        .formatted(employeeId, UUID.randomUUID()))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PARTIAL"))
        .andExpect(jsonPath("$.pendingAmount").value(800.0));
  }

  private static long extractLongId(String json, String path) {
    Number n = JsonPath.read(json, path);
    return n.longValue();
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-payroll-" + UUID.randomUUID() + "@mail.com";
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
