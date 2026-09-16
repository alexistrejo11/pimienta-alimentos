package io.github.alexistrejo11.pimienta.module.pos.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import io.github.alexistrejo11.pimienta.module.inventory.core.application.command.ApplyPosSaleStockCommand;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosLocationUseCases;
import io.github.alexistrejo11.pimienta.module.inventory.core.port.input.PosSaleInventoryUseCases;
import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosChangeLogRepository;
import io.github.alexistrejo11.pimienta.module.pos.infrastructure.adapter.output.persistence.repository.PosChangeLogSpringDataRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PosSyncConcurrencyIntegrationTest {

  private static final WatermarkGate GATE = new WatermarkGate();

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private PosSaleInventoryUseCases posSaleInventoryUseCases;
  @Autowired private PosLocationUseCases posLocationUseCases;
  @Autowired private PosChangeLogSpringDataRepository posChangeLogRepository;

  @AfterEach
  void releaseGate() {
    GATE.enabled = false;
    GATE.release.countDown();
  }

  @Test
  void bootstrapWatermark_doesNotLoseConcurrentCatalogAndStockMutations() throws Exception {
    String staffToken = obtainAccessToken();
    long hqId = createHeadquarter(staffToken, "POS-CONCURRENT-" + UUID.randomUUID());
    putPosSettings(staffToken, hqId);
    long itemId = createItem(staffToken, "SKU-CONCURRENT-" + UUID.randomUUID(), "Concurrent item");
    putCatalog(staffToken, hqId, itemId, "Bebidas", "25.00");
    posLocationUseCases.ensurePosLocation(hqId);

    String deviceAccess = enrollDevice(staffToken, hqId, "Concurrent caja");
    GATE.enable();

    ExecutorService executor = Executors.newFixedThreadPool(3);
    try {
      Future<MvcResult> bootstrap =
          executor.submit(
              () ->
                  mockMvc
                      .perform(
                          AccountTestRequests.getBearer(
                              "/api/v1/pos/sync/bootstrap", deviceAccess))
                      .andExpect(status().isOk())
                      .andReturn());

      org.junit.jupiter.api.Assertions.assertTrue(
          GATE.watermarkObserved.await(10, TimeUnit.SECONDS),
          "bootstrap did not capture its watermark");

      Future<?> catalogMutation =
          executor.submit(
              () -> {
                try {
                  putCatalog(staffToken, hqId, itemId, "Bebidas", "28.00");
                } catch (Exception ex) {
                  throw new RuntimeException(ex);
                }
              });
      Future<?> stockMutation =
          executor.submit(
              () ->
                  posSaleInventoryUseCases.applySaleStock(
                      new ApplyPosSaleStockCommand(
                          hqId, itemId, -1, "concurrent-stock", null, null)));

      catalogMutation.get(10, TimeUnit.SECONDS);
      stockMutation.get(10, TimeUnit.SECONDS);
      GATE.release.countDown();

      MvcResult bootstrapResult = bootstrap.get(10, TimeUnit.SECONDS);
      String cursor = JsonPath.read(bootstrapResult.getResponse().getContentAsString(), "$.cursors.changes");
      long initialSequence = Long.parseLong(cursor.substring(cursor.lastIndexOf('s') + 1));
      GATE.disable();
      long committedUpperBound =
          posChangeLogRepository
              .findFirstByHeadquarterIdOrderBySequenceDesc(hqId)
              .orElseThrow(() -> new AssertionError("change log is empty"))
              .getSequence();
      int committedChangeCount = (int) (committedUpperBound - initialSequence);

      MvcResult changes =
          mockMvc
              .perform(AccountTestRequests.getBearer(changesUrl(cursor), deviceAccess))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.operations", hasSize(committedChangeCount)))
              .andExpect(jsonPath("$.operations[?(@.entity=='product')]").isNotEmpty())
              .andExpect(
                  jsonPath("$.nextCursor").value("cursor-hq-" + hqId + "-s" + committedUpperBound))
              .andReturn();

      String nextCursor = JsonPath.read(changes.getResponse().getContentAsString(), "$.nextCursor");
      mockMvc
          .perform(AccountTestRequests.getBearer(changesUrl(nextCursor), deviceAccess))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.operations", hasSize(0)))
          .andExpect(jsonPath("$.nextCursor").value(nextCursor));
    } finally {
      GATE.disable();
      GATE.release.countDown();
      executor.shutdownNow();
    }
  }

  private static String changesUrl(String cursor) {
    return "/api/v1/pos/sync/changes?cursor="
        + URLEncoder.encode(cursor, StandardCharsets.UTF_8);
  }

  private void putPosSettings(String token, long hqId) throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-settings",
                token,
                """
                {
                  "currency": "MXN",
                  "catalogStaleWarnHours": 24,
                  "catalogStaleBlockHours": 72,
                  "openAmountCategories": ["MISC"],
                  "defaultNegativeStockLimit": 10
                }
                """))
        .andExpect(status().isOk());
  }

  private void putCatalog(String token, long hqId, long itemId, String category, String price)
      throws Exception {
    mockMvc
        .perform(
            AccountTestRequests.putJsonBearer(
                "/api/v1/headquarters/" + hqId + "/pos-catalog/" + itemId,
                token,
                """
                {
                  "saleCategory": "%s",
                  "salePrice": %s,
                  "available": true,
                  "stockPolicy": "CONTROLLED",
                  "negativeStockLimit": 5
                }
                """.formatted(category, price)))
        .andExpect(status().isOk());
  }

  private long createItem(String token, String sku, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/inventory/items",
                    token,
                    """
                    {
                      "sku": "%s",
                      "name": "%s",
                      "description": "IT",
                      "costPrice": 10.00,
                      "category": "CONSUMABLE",
                      "unit": "PIECE",
                      "reorderPoint": 0,
                      "reorderQuantity": 0
                    }
                    """.formatted(sku, name)))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  private long createHeadquarter(String token, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/headquarters",
                    token,
                    "{\"name\":\"%s\",\"address\":\"Addr\",\"description\":\"d\"}"
                        .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  private String enrollDevice(String staffToken, long hqId, String name) throws Exception {
    MvcResult code =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/pos/admin/enrollment-codes",
                    staffToken,
                    "{\"headquarterId\":%d}".formatted(hqId)))
            .andExpect(status().isOk())
            .andReturn();
    String enrollmentCode = JsonPath.read(code.getResponse().getContentAsString(), "$.code");
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/pos/devices/enroll",
                    """
                    {
                      "enrollmentCode": "%s",
                      "devicePublicId": "%s",
                      "deviceName": "%s",
                      "appVersion": "1.0.0"
                    }
                    """.formatted(enrollmentCode, UUID.randomUUID(), name)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
  }

  private String obtainAccessToken() throws Exception {
    String email = "it-pos-concurrency-" + UUID.randomUUID() + "@mail.com";
    String phone = "+52" + String.format("%010d", Math.abs(System.nanoTime()) % 10_000_000_000L);
    String password = "Str0ngPass!";
    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/register",
                AccountTestRequests.validRegisterJson(email, phone, password)))
        .andExpect(status().isCreated());
    UserJpaEntity user =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new AssertionError("user missing"));
    user.setAccountStatus(AccountStatus.ACTIVE);
    user.setRoles(new LinkedHashSet<>(Set.of(Role.ADMIN)));
    userJpaRepository.saveAndFlush(user);
    MvcResult login =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
  }

  private static final class WatermarkGate {
    private volatile boolean enabled;
    private volatile CountDownLatch watermarkObserved = new CountDownLatch(1);
    private volatile CountDownLatch release = new CountDownLatch(1);

    void enable() {
      watermarkObserved = new CountDownLatch(1);
      release = new CountDownLatch(1);
      enabled = true;
    }

    void disable() {
      enabled = false;
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class ConcurrencyTestConfiguration {
    @Bean
    @Primary
    PosChangeLogRepository gatedPosChangeLogRepository(
        @Qualifier("posChangeLogRepositoryImpl") PosChangeLogRepository delegate) {
      return new PosChangeLogRepository() {
        @Override
        public PosChangeLogEntry save(PosChangeLogEntry entry) {
          return delegate.save(entry);
        }

        @Override
        public java.util.Optional<PosChangeLogEntry> findFirstByHeadquarterId(long headquarterId) {
          return delegate.findFirstByHeadquarterId(headquarterId);
        }

        @Override
        public java.util.Optional<PosChangeLogEntry> findLastByHeadquarterId(long headquarterId) {
          java.util.Optional<PosChangeLogEntry> result = delegate.findLastByHeadquarterId(headquarterId);
          if (GATE.enabled) {
            GATE.watermarkObserved.countDown();
            try {
              if (!GATE.release.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("concurrent mutation was not released");
              }
            } catch (InterruptedException ex) {
              Thread.currentThread().interrupt();
              throw new AssertionError("bootstrap watermark gate interrupted", ex);
            }
          }
          return result;
        }

        @Override
        public java.util.List<PosChangeLogEntry> findAfterSequence(
            long headquarterId, long sequence, long upperBound, int limit) {
          return delegate.findAfterSequence(headquarterId, sequence, upperBound, limit);
        }
      };
    }
  }
}
