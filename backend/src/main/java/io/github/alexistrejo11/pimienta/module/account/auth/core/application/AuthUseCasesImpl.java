package io.github.alexistrejo11.pimienta.module.account.auth.core.application;

import io.github.alexistrejo11.pimienta.module.account.auth.core.application.RegisterResult;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.LoginCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.LogoutCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.RefreshSessionCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.application.command.RegisterCommand;
import io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity.IssuedTokens;
import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.AuthUseCases;
import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.RefreshTokenStore;
import io.github.alexistrejo11.pimienta.module.account.auth.core.port.input.TokenService;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.UserRegisterParams;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.exceptions.AccountPendingApprovalException;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.exceptions.EmailAlreadyExistsException;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.exceptions.PhoneAlreadyExistsException;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.exceptions.UserNotFoundException;
import io.github.alexistrejo11.pimienta.module.account.user.core.port.output.UserRepository;
import io.github.alexistrejo11.pimienta.module.notification.core.application.event.AccountPendingApprovalEvent;
import io.github.alexistrejo11.pimienta.module.notification.core.port.output.DomainEventPublisher;
import io.github.alexistrejo11.pimienta.shared.exception.AuthenticationException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthUseCasesImpl implements AuthUseCases {
  private static final Logger log = LoggerFactory.getLogger(AuthUseCasesImpl.class);
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final RefreshTokenStore refreshTokenStore;
  private final DomainEventPublisher domainEventPublisher;

  public AuthUseCasesImpl(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      RefreshTokenStore refreshTokenStore,
      DomainEventPublisher domainEventPublisher) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.refreshTokenStore = refreshTokenStore;
    this.domainEventPublisher = domainEventPublisher;
  }

  @Override
  @Transactional
  public RegisterResult register(RegisterCommand command) {
    log.info("register start {}", command.toStringMasked());

    if (userRepository.findByEmail(command.email()).isPresent()) {
      throw new EmailAlreadyExistsException(command.email());
    }

    if (command.phone() != null) {
      if (userRepository.findByPhone(command.phone()).isPresent()) {
        throw new PhoneAlreadyExistsException(command.phone());
      }
    }

    log.debug("register uniqueness ok email={}", command.email());

    String hash = passwordEncoder.encode(command.password());
    log.debug("register password hashed email={}", command.email());

    UserRegisterParams params = UserRegisterParams.builder()
        .email(command.email())
        .passwordHash(hash)
        .firstName(command.firstName())
        .lastName(command.lastName())
        .gender(command.gender())
        .phone(command.phone())
        .dateOfBirth(command.dateOfBirth())
        .build();

    User user = User.register(params);

    User saved = userRepository.save(user);
    log.info("register complete email={} userId={}", command.email(), saved.getId());

    domainEventPublisher.publish(new AccountPendingApprovalEvent(
        saved.getId(),
        saved.getEmail(),
        saved.getFirstName(),
        saved.getLastName(),
        saved.getPhone(),
        Instant.now()));

    return new RegisterResult(true);
  }

  @Override
  @Transactional(readOnly = true)
  public IssuedTokens login(LoginCommand command) {
    log.info("login start {}", command.describeForLog());
    if (command.email() == null || command.email().isBlank()) {
      throw new AuthenticationException(
          "Invalid email or password.",
          Map.of(),
          "Login failed: missing email");
    }
    String email = command.email().trim().toLowerCase();

    log.debug("login lookup email={}", email);

    User user = userRepository
        .findByEmail(email)
        .orElseThrow(
            () -> new AuthenticationException(
                "Invalid email or password.",
                Map.of("email", email),
                "Login failed: user not found " + email));

    log.debug("login user found userId={} email={}", user.getId(), email);

    if (user.isPendingApproval()) {
      log.info("login blocked pending approval userId={}", user.getId());
      throw new AccountPendingApprovalException(user.getId());
    }

    if (user.isBanned()) {
      log.info("login blocked banned userId={}", user.getId());
      throw new AuthenticationException(
          "This account has been suspended.",
          Map.of("userId", user.getId()),
          "Login blocked: banned user id=" + user.getId());
    }

    log.debug("login verify credentials email={}", email);

    if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
      throw new AuthenticationException(
          "Invalid email or password.",
          Map.of("email", email),
          "Login failed: bad password for " + email);
    }

    IssuedTokens tokens = tokenService.issuePair(user);

    log.info("login success userId={} email={}", user.getId(), email);
    return tokens;
  }

  @Override
  @Transactional(readOnly = true)
  public IssuedTokens refresh(RefreshSessionCommand command) {
    log.info("refresh start refreshToken={}", describeRefreshToken(command.refreshToken()));
    var validated = tokenService.validateRefreshToken(command.refreshToken());

    User user = userRepository
        .findById(validated.userId())
        .orElseThrow(() -> new UserNotFoundException(validated.userId()));

    if (user.isBanned()) {
      log.info("refresh blocked banned userId={}", validated.userId());
      refreshTokenStore.remove(validated.jti());
      throw new AuthenticationException(
          "This account has been suspended.",
          Map.of("userId", validated.userId()),
          "Refresh blocked: banned user id=" + validated.userId());
    }

    String newAccess = tokenService.issueAccessToken(user);
    IssuedTokens tokens =
        new IssuedTokens(
            newAccess, command.refreshToken(), tokenService.accessTokenExpiresInSeconds());

    log.info("refresh success userId={}", user.getId());
    return tokens;
  }

  @Override
  @Transactional(readOnly = true)
  public void logout(LogoutCommand command) {
    log.info("logout start refreshToken={}", describeRefreshToken(command.refreshToken()));

    if (command.refreshToken() == null || command.refreshToken().isBlank()) {
      log.info("logout noop no refresh token");
      return;
    }

    String jti = tokenService.extractJtiFromRefreshToken(command.refreshToken());
    if (jti != null) {
      refreshTokenStore.remove(jti);
    }
    log.info("logout complete refresh session revoked");
  }

  /** Never logs token bytes — only presence and length. */
  private static String describeRefreshToken(String refreshToken) {
    if (refreshToken == null) {
      return "absent";
    }
    if (refreshToken.isBlank()) {
      return "blank";
    }
    return "present(len=" + refreshToken.length() + ")";
  }
}
