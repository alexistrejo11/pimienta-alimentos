package io.github.alexistrejo11.pimienta.module.account.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.AccountStatus;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.core.port.input.AdminBootstrapUseCases;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaEntity;
import io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.out.persistence.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "pimienta.bootstrap.admin.enabled=true",
      "pimienta.bootstrap.admin.email=bootstrap-admin@pimienta.test",
      "pimienta.bootstrap.admin.password=BootstrapPass1",
      "pimienta.bootstrap.admin.first-name=Seed",
      "pimienta.bootstrap.admin.last-name=Admin",
      "pimienta.bootstrap.admin.phone=+529991110001"
    })
class AdminBootstrapIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private AdminBootstrapUseCases adminBootstrapUseCases;

  @Test
  void startup_createsActiveAdminOnce_andAllowsLogin() throws Exception {
    UserJpaEntity saved =
        userJpaRepository
            .findByEmailAndDeletedAtIsNull("bootstrap-admin@pimienta.test")
            .orElseThrow(() -> new AssertionError("Bootstrap admin was not persisted"));

    assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    assertThat(saved.getRoles()).contains(Role.ADMIN);
    assertThat(saved.getFirstName()).isEqualTo("Seed");
    assertThat(saved.getLastName()).isEqualTo("Admin");

    mockMvc
        .perform(
            AccountTestRequests.postJson(
                "/api/v1/auth/login",
                AccountTestRequests.loginJson(
                    "bootstrap-admin@pimienta.test", "BootstrapPass1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty());

    assertThat(adminBootstrapUseCases.ensureInitialAdmin()).isFalse();
    assertThat(userJpaRepository.findByEmailAndDeletedAtIsNull("bootstrap-admin@pimienta.test"))
        .isPresent();
  }
}
