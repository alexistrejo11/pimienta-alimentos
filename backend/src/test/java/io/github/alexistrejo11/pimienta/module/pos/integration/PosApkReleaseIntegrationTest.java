package io.github.alexistrejo11.pimienta.module.pos.integration;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexistrejo11.pimienta.module.pos.core.port.output.PosApkReleaseStoragePort;
import io.github.alexistrejo11.pimienta.module.pos.integration.support.StubPosApkReleaseStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import(PosApkReleaseIntegrationTest.StubPosApkStorageConfig.class)
class PosApkReleaseIntegrationTest {

  private static final String LATEST = "/api/v1/pos/releases/android/latest";

  @Autowired private MockMvc mockMvc;

  @Autowired private PosApkReleaseStoragePort storagePort;

  @TestConfiguration
  static class StubPosApkStorageConfig {

    @Bean
    @Primary
    PosApkReleaseStoragePort posApkReleaseStoragePort() {
      return new StubPosApkReleaseStoragePort();
    }
  }

  @BeforeEach
  void resetStub() {
    StubPosApkReleaseStoragePort stub = (StubPosApkReleaseStoragePort) storagePort;
    stub.setManifest(
        new io.github.alexistrejo11.pimienta.module.pos.core.domain.PosApkManifest(
            "1.0", 1, "pimienta/releases/pos/android/latest.apk"));
  }

  @Test
  void latest_withoutAuth_returns200WithPresignedUrl() throws Exception {
    mockMvc
        .perform(get(LATEST))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.versionName").value("1.0"))
        .andExpect(jsonPath("$.versionCode").value(1))
        .andExpect(jsonPath("$.expiresInSeconds").value(86400))
        .andExpect(
            jsonPath(
                "$.url",
                startsWith(
                    StubPosApkReleaseStoragePort.PRESIGN_BASE
                        + "pimienta/releases/pos/android/latest.apk")));
  }

  @Test
  void latest_whenManifestMissing_returns404() throws Exception {
    ((StubPosApkReleaseStoragePort) storagePort).clearManifest();

    mockMvc
        .perform(get(LATEST))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("POS_APK_NOT_FOUND"));
  }
}
