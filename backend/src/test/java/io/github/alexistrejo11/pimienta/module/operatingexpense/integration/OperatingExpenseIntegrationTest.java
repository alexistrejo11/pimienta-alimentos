package io.github.alexistrejo11.pimienta.module.operatingexpense.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.github.alexistrejo11.pimienta.module.account.integration.AccountTestRequests;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.OperatingExpenseAccessPolicy;
import io.github.alexistrejo11.pimienta.module.operatingexpense.integration.support.StubOperatingExpenseReceiptStorage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(OperatingExpenseIntegrationTest.StubStorageConfig.class)
class OperatingExpenseIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private StubOperatingExpenseReceiptStorage storage;

  private final LocalDate today = LocalDate.now(OperatingExpenseAccessPolicy.BUSINESS_ZONE);

  @BeforeEach
  void clearStorage() {
    storage.clear();
  }

  @TestConfiguration
  static class StubStorageConfig {
    @Bean
    @Primary
    StubOperatingExpenseReceiptStorage expenseReceiptStorage(@Value("${aws.s3.folder}") String folder) {
      return new StubOperatingExpenseReceiptStorage(folder);
    }
  }

  @Test
  void listAndCreate_withoutToken_return401() throws Exception {
    mockMvc.perform(get("/api/v1/operating-expenses")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(multipart("/api/v1/operating-expenses").file(jsonPart(body(1L, "Hielo", today, null))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void salesSupportAndUser_areForbidden() throws Exception {
    for (Role role : List.of(Role.SALES, Role.SUPPORT, Role.USER)) {
      TokenPair token = obtainToken(Set.of(role));
      mockMvc
          .perform(AccountTestRequests.getBearer("/api/v1/operating-expenses", token.token()))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }
  }

  @Test
  void adminAndDirector_haveUnrestrictedCrud() throws Exception {
    for (Role role : List.of(Role.ADMIN, Role.DIRECTOR)) {
      storage.clear();
      TokenPair admin = obtainToken(Set.of(Role.ADMIN));
      long hq = createHeadquarter(admin.token(), "EXP-" + role + "-" + UUID.randomUUID());
      TokenPair actor = role == Role.ADMIN ? admin : obtainToken(Set.of(role));

      MvcResult created =
          mockMvc
              .perform(create(actor.token(), body(hq, "Hielo " + role, today.minusDays(1), null), jpeg()))
              .andExpect(status().isCreated())
              .andExpect(jsonPath("$.concept").value("Hielo " + role))
              .andExpect(jsonPath("$.occurredOn").value(today.minusDays(1).toString()))
              .andExpect(jsonPath("$.attachments.length()").value(1))
              .andExpect(jsonPath("$.attachments[0].url").value(org.hamcrest.Matchers.startsWith("https://s3.test/")))
              .andReturn();
      long id = idOf(created);
      String key = JsonPath.read(created.getResponse().getContentAsString(), "$.attachments[0].key");

      mockMvc
          .perform(
              AccountTestRequests.getBearer(
                  "/api/v1/operating-expenses?headquarterId="
                      + hq
                      + "&from="
                      + today.minusDays(1)
                      + "&to="
                      + today.minusDays(1)
                      + "&search=hielo",
                  actor.token()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items[0].id").value((int) id));

      mockMvc
          .perform(
              AccountTestRequests.getBearer(
                  "/api/v1/operating-expenses/summary?headquarterId="
                      + hq
                      + "&from="
                      + today.minusDays(1)
                      + "&to="
                      + today.minusDays(1),
                  actor.token()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.totalAmount").value(25.50));

      mockMvc
          .perform(
              update(actor.token(), id, body(hq, "Hielo corregido", today.minusDays(2), List.of(key))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.concept").value("Hielo corregido"))
          .andExpect(jsonPath("$.attachments[0].key").value(key));

      mockMvc
          .perform(delete("/api/v1/operating-expenses/" + id).header("Authorization", "Bearer " + actor.token()))
          .andExpect(status().isNoContent());
      mockMvc
          .perform(AccountTestRequests.getBearer("/api/v1/operating-expenses/" + id, actor.token()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.errorCode").value("OPERATING_EXPENSE_NOT_FOUND"));
      org.junit.jupiter.api.Assertions.assertTrue(storage.contains(key));
      org.junit.jupiter.api.Assertions.assertTrue(storage.deletedKeys().isEmpty());
    }
  }

  @Test
  void managerAndEmployee_areLimitedToTodayAndAssignedSede() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq = createHeadquarter(admin.token(), "EXP-HQ-" + UUID.randomUUID());
    long other = createHeadquarter(admin.token(), "EXP-OTHER-" + UUID.randomUUID());

    MvcResult historical =
        mockMvc
            .perform(create(admin.token(), body(hq, "Ayer", today.minusDays(1), null)))
            .andExpect(status().isCreated())
            .andReturn();
    long historicalId = idOf(historical);

    for (Role role : List.of(Role.MANAGER, Role.EMPLOYEE)) {
      TokenPair staff = obtainToken(Set.of(role));
      assignHeadquarters(admin.token(), staff.userId(), List.of(hq));

      mockMvc
          .perform(create(staff.token(), body(null, "Hoy " + role, null, null), jpeg()))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.headquarterId").value((int) hq))
          .andExpect(jsonPath("$.occurredOn").value(today.toString()));

      mockMvc
          .perform(AccountTestRequests.getBearer("/api/v1/operating-expenses?search=hoy", staff.token()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.items[0].concept").value("Hoy " + role));

      mockMvc
          .perform(AccountTestRequests.getBearer("/api/v1/operating-expenses/" + historicalId, staff.token()))
          .andExpect(status().isForbidden());
      mockMvc
          .perform(
              create(staff.token(), body(other, "Otra sede", today, null)))
          .andExpect(status().isForbidden());
      mockMvc
          .perform(create(staff.token(), body(hq, "Ayer", today.minusDays(1), null)))
          .andExpect(status().isForbidden());
      mockMvc
          .perform(
              AccountTestRequests.getBearer(
                  "/api/v1/operating-expenses?from=" + today.minusDays(1), staff.token()))
          .andExpect(status().isForbidden());
      mockMvc
          .perform(
              AccountTestRequests.getBearer(
                  "/api/v1/operating-expenses/summary?from=" + today.minusDays(1), staff.token()))
          .andExpect(status().isForbidden());

      MvcResult own =
          mockMvc
              .perform(create(staff.token(), body(hq, "Propio " + role, today, null)))
              .andExpect(status().isCreated())
              .andReturn();
      long ownId = idOf(own);
      mockMvc
          .perform(update(staff.token(), ownId, body(hq, "Editado", today, List.of())))
          .andExpect(status().isForbidden());
      mockMvc
          .perform(delete("/api/v1/operating-expenses/" + ownId).header("Authorization", "Bearer " + staff.token()))
          .andExpect(status().isForbidden());
    }
  }

  @Test
  void staffWithSeveralSedes_mustChooseOne() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq1 = createHeadquarter(admin.token(), "EXP-M1-" + UUID.randomUUID());
    long hq2 = createHeadquarter(admin.token(), "EXP-M2-" + UUID.randomUUID());
    TokenPair manager = obtainToken(Set.of(Role.MANAGER));
    assignHeadquarters(admin.token(), manager.userId(), List.of(hq1, hq2));

    mockMvc
        .perform(create(manager.token(), body(null, "Sin sede", today, null)))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(create(manager.token(), body(hq2, "Sede dos", today, null)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.headquarterId").value((int) hq2));
  }

  @Test
  void summaryGroupsEveryMatchingDayAndBreaksCategoryTies() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq = createHeadquarter(admin.token(), "EXP-SUM-" + UUID.randomUUID());
    LocalDate first = today.minusDays(1);
    mockMvc.perform(create(admin.token(), expenseJson(hq, "Hielo renta", first, "RENT", "20.00"))).andExpect(status().isCreated());
    mockMvc
        .perform(create(admin.token(), expenseJson(hq, "Hielo proveedores", first, "SUPPLIERS_MERCHANDISE", "10.00")))
        .andExpect(status().isCreated());
    mockMvc
        .perform(create(admin.token(), expenseJson(hq, "Hielo proveedores", today, "SUPPLIERS_MERCHANDISE", "10.00")))
        .andExpect(status().isCreated());
    mockMvc
        .perform(create(admin.token(), expenseJson(hq, "Otro concepto", today, "OTHER", "100.00")))
        .andExpect(status().isCreated());

    String summary =
        "/api/v1/operating-expenses/summary?headquarterId=" + hq + "&from=" + first + "&to=" + today;
    mockMvc
        .perform(AccountTestRequests.getBearer(summary + "&search=hielo", admin.token()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalAmount").value(40.0))
        .andExpect(jsonPath("$.topCategory.category").value("SUPPLIERS_MERCHANDISE"))
        .andExpect(jsonPath("$.topCategory.totalAmount").value(20.0))
        .andExpect(jsonPath("$.topCategory.count").value(2))
        .andExpect(jsonPath("$.byDay.length()").value(2))
        .andExpect(jsonPath("$.byDay[0].occurredOn").value(first.toString()))
        .andExpect(jsonPath("$.byDay[0].totalAmount").value(30.0))
        .andExpect(jsonPath("$.byDay[0].count").value(2))
        .andExpect(jsonPath("$.byDay[1].occurredOn").value(today.toString()))
        .andExpect(jsonPath("$.byDay[1].totalAmount").value(10.0))
        .andExpect(jsonPath("$.byDay[1].count").value(1));
  }

  @Test
  void rejectedImage_returns400_andPutReplacesReceiptsWithoutLosingRetainedOnes() throws Exception {
    TokenPair admin = obtainToken(Set.of(Role.ADMIN));
    long hq = createHeadquarter(admin.token(), "EXP-IMG-" + UUID.randomUUID());

    mockMvc
        .perform(
            multipart("/api/v1/operating-expenses")
                .file(jsonPart(body(hq, "Mala", today, null)))
                .file(new MockMultipartFile("attachments", "note.jpg", "image/jpeg", pngBytes()))
                .header("Authorization", "Bearer " + admin.token()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

    MvcResult created =
        mockMvc
            .perform(
                create(
                    admin.token(),
                    body(hq, "Dos notas", today, null),
                    jpeg(),
                    jpeg()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.attachments.length()").value(2))
            .andReturn();
    long id = idOf(created);
    String keep = JsonPath.read(created.getResponse().getContentAsString(), "$.attachments[0].key");
    String drop = JsonPath.read(created.getResponse().getContentAsString(), "$.attachments[1].key");

    MvcResult updated =
        mockMvc
            .perform(update(admin.token(), id, body(hq, "Dos notas", today, List.of(keep)), jpeg()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.attachments.length()").value(2))
            .andReturn();
    String added = JsonPath.read(updated.getResponse().getContentAsString(), "$.attachments[1].key");

    org.junit.jupiter.api.Assertions.assertTrue(storage.contains(keep));
    org.junit.jupiter.api.Assertions.assertTrue(storage.contains(added));
    org.junit.jupiter.api.Assertions.assertFalse(storage.contains(drop));
    org.junit.jupiter.api.Assertions.assertTrue(storage.deletedKeys().contains(drop));

    mockMvc
        .perform(delete("/api/v1/operating-expenses/" + id).header("Authorization", "Bearer " + admin.token()))
        .andExpect(status().isNoContent());
    org.junit.jupiter.api.Assertions.assertTrue(storage.contains(keep));
    org.junit.jupiter.api.Assertions.assertTrue(storage.contains(added));
  }

  private MockMultipartHttpServletRequestBuilder create(String token, String json, byte[]... images) {
    MockMultipartHttpServletRequestBuilder builder = multipart("/api/v1/operating-expenses");
    builder.file(jsonPart(json));
    builder.header("Authorization", "Bearer " + token);
    addImages(builder, images);
    return builder;
  }

  private MockMultipartHttpServletRequestBuilder update(String token, long id, String json, byte[]... images) {
    MockMultipartHttpServletRequestBuilder builder = multipart(HttpMethod.PUT, "/api/v1/operating-expenses/" + id);
    builder.file(jsonPart(json));
    builder.header("Authorization", "Bearer " + token);
    addImages(builder, images);
    return builder;
  }

  private static void addImages(MockMultipartHttpServletRequestBuilder builder, byte[]... images) {
    for (int i = 0; i < images.length; i++) {
      String type = images[i].length > 2 && images[i][0] == (byte) 0x89 ? "image/png" : "image/jpeg";
      String name = type.endsWith("png") ? "note.png" : "note-" + i + ".jpg";
      builder.file(new MockMultipartFile("attachments", name, type, images[i]));
    }
  }

  private static MockMultipartFile jsonPart(String json) {
    return new MockMultipartFile("data", "data", MediaType.APPLICATION_JSON_VALUE, json.getBytes(StandardCharsets.UTF_8));
  }

  private static String expenseJson(long headquarterId, String concept, LocalDate day, String category, String amount) {
    return "{\"amount\":"
        + amount
        + ",\"category\":\""
        + category
        + "\",\"concept\":\""
        + concept
        + "\",\"headquarterId\":"
        + headquarterId
        + ",\"occurredOn\":\""
        + day
        + "\"}";
  }

  private static String body(Long headquarterId, String concept, LocalDate day, List<String> retain) {
    StringBuilder json = new StringBuilder();
    json.append("{\"amount\":25.50,\"category\":\"SUPPLIERS_MERCHANDISE\",\"concept\":\"")
        .append(concept)
        .append("\",\"notes\":\"nota\"");
    if (headquarterId != null) {
      json.append(",\"headquarterId\":").append(headquarterId);
    }
    if (day != null) {
      json.append(",\"occurredOn\":\"").append(day).append('"');
    }
    if (retain != null) {
      json.append(",\"retainAttachmentKeys\":[");
      for (int i = 0; i < retain.size(); i++) {
        if (i > 0) {
          json.append(',');
        }
        json.append('"').append(retain.get(i)).append('"');
      }
      json.append(']');
    }
    json.append('}');
    return json.toString();
  }

  private static long idOf(MvcResult result) throws Exception {
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  private static byte[] jpeg() throws Exception {
    BufferedImage image = new BufferedImage(12, 8, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.RED);
    graphics.fillRect(0, 0, 12, 8);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", out);
    return out.toByteArray();
  }

  private static byte[] pngBytes() throws Exception {
    BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.BLUE);
    graphics.fillRect(0, 0, 8, 8);
    graphics.dispose();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(image, "png", out);
    return out.toByteArray();
  }

  private record TokenPair(String token, Long userId) {}

  private TokenPair obtainToken(Set<Role> roles) throws Exception {
    String email = "it-exp-" + UUID.randomUUID() + "@mail.com";
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
    UserJpaEntity user =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull(email)
            .orElseThrow(() -> new AssertionError("user missing"));
    user.setAccountStatus(AccountStatus.ACTIVE);
    user.setRoles(new LinkedHashSet<>(roles));
    userJpaRepository.saveAndFlush(user);
    MvcResult login =
        mockMvc
            .perform(
                AccountTestRequests.postJson(
                    "/api/v1/auth/login", AccountTestRequests.loginJson(email, password)))
            .andExpect(status().isOk())
            .andReturn();
    String token = JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
    return new TokenPair(token, user.getId());
  }

  private long createHeadquarter(String token, String name) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                AccountTestRequests.postJsonBearer(
                    "/api/v1/headquarters",
                    token,
                    """
                    {"name":"%s","address":"Test 1","description":"OperatingExpense IT"}
                    """
                        .formatted(name)))
            .andExpect(status().isCreated())
            .andReturn();
    return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
  }

  private void assignHeadquarters(String adminToken, long userId, List<Long> headquarterIds)
      throws Exception {
    String ids =
        headquarterIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
    mockMvc
        .perform(
            AccountTestRequests.postJsonBearer(
                "/api/v1/users/management/" + userId + "/headquarters",
                adminToken,
                "{\"headquarterIds\":[" + ids + "]}"))
        .andExpect(status().isOk());
  }
}
