package io.github.alexistrejo11.pimienta.module.account.user.core.application;

import io.github.alexistrejo11.pimienta.config.bootstrap.AdminBootstrapProperties;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.UserRegisterParams;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Gender;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.enums.Role;
import io.github.alexistrejo11.pimienta.module.account.user.core.port.input.AdminBootstrapUseCases;
import io.github.alexistrejo11.pimienta.module.account.user.core.port.output.UserRepository;
import io.github.alexistrejo11.pimienta.shared.validation.PasswordStrengthValidator;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapUseCasesImpl implements AdminBootstrapUseCases {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrapUseCasesImpl.class);
  private static final LocalDate PLACEHOLDER_DATE_OF_BIRTH = LocalDate.of(1990, 1, 1);
  private static final PasswordStrengthValidator PASSWORD_STRENGTH = new PasswordStrengthValidator();

  private final AdminBootstrapProperties properties;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminBootstrapUseCasesImpl(
      AdminBootstrapProperties properties,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.properties = properties;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public boolean ensureInitialAdmin() {
    if (!properties.isEnabled()) {
      log.debug("admin bootstrap disabled");
      return false;
    }
    if (!properties.hasCredentials()) {
      log.info("Admin bootstrap skipped: PIMIENTA_BOOTSTRAP_ADMIN_EMAIL and PASSWORD are not set");
      return false;
    }

    String email = properties.getEmail().trim().toLowerCase();
    String password = properties.getPassword();
    if (!PASSWORD_STRENGTH.isValid(password, null)) {
      log.error(
          "Admin bootstrap skipped: password must be 8–128 characters and contain both a letter and a digit");
      return false;
    }

    if (userRepository.findByEmail(email).isPresent()) {
      log.info("Admin bootstrap skipped: account already exists for {}", email);
      return false;
    }
    if (!userRepository.findActiveByRole(Role.ADMIN).isEmpty()) {
      log.info("Admin bootstrap skipped: an ACTIVE ADMIN already exists");
      return false;
    }

    String phone = blankToDefault(properties.getPhone(), "+520000000001");
    if (userRepository.findByPhone(phone).isPresent()) {
      log.error(
          "Admin bootstrap skipped: phone already in use; set PIMIENTA_BOOTSTRAP_ADMIN_PHONE to a free number");
      return false;
    }

    User user =
        User.register(
            UserRegisterParams.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(blankToDefault(properties.getFirstName(), "Admin"))
                .lastName(blankToDefault(properties.getLastName(), "Pimienta"))
                .gender(Gender.PREFER_NOT_TO_SAY)
                .phone(phone)
                .dateOfBirth(PLACEHOLDER_DATE_OF_BIRTH)
                .build());
    user.activate();
    user.addRoles(List.of(Role.ADMIN));
    User saved = userRepository.save(user);

    log.info("Admin bootstrap created ACTIVE ADMIN userId={} email={}", saved.getId(), email);
    return true;
  }

  private static String blankToDefault(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    return value.trim();
  }
}
