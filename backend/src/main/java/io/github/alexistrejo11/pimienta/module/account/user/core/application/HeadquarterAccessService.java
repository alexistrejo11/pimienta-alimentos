package io.github.alexistrejo11.pimienta.module.account.user.core.application;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.entities.User;
import io.github.alexistrejo11.pimienta.module.account.user.core.domain.exceptions.UserNotFoundException;
import io.github.alexistrejo11.pimienta.module.account.user.core.port.output.UserRepository;
import io.github.alexistrejo11.pimienta.shared.exception.ForbiddenException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enforces headquarter scope for MANAGER web staff; ADMIN bypasses checks. */
@Service
public class HeadquarterAccessService {

  private final UserRepository userRepository;

  public HeadquarterAccessService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public void requireHeadquarterAccess(JwtAuthenticationContext principal, long headquarterId) {
    if (isAdmin(principal)) {
      return;
    }
    if (isScopedStaff(principal) && assignedHeadquarters(principal).contains(headquarterId)) {
      return;
    }
    throw forbidden(headquarterId);
  }

  @Transactional(readOnly = true)
  public void requireHeadquarterAccessIfPresent(
      JwtAuthenticationContext principal, Long headquarterId) {
    if (headquarterId == null) {
      requireGlobalPosAccess(principal);
      return;
    }
    requireHeadquarterAccess(principal, headquarterId);
  }

  /** MANAGER must pass an assigned HQ; ADMIN may omit for global views. */
  @Transactional(readOnly = true)
  public void requireGlobalPosAccess(JwtAuthenticationContext principal) {
    if (isAdmin(principal)) {
      return;
    }
    throw new ForbiddenException(
        "Global POS access requires ADMIN role",
        Map.of(),
        "userId=" + principal.userId() + " attempted global POS access");
  }

  @Transactional(readOnly = true)
  public List<Long> assignedHeadquarters(JwtAuthenticationContext principal) {
    return loadUser(principal).getAssignedHeadquarterIds();
  }

  @Transactional(readOnly = true)
  public boolean isAdmin(JwtAuthenticationContext principal) {
    return principal.roles().stream().anyMatch(r -> "ADMIN".equals(r) || "ROLE_ADMIN".equals(r));
  }

  @Transactional(readOnly = true)
  public boolean isManager(JwtAuthenticationContext principal) {
    return principal.roles().stream().anyMatch(r -> "MANAGER".equals(r) || "ROLE_MANAGER".equals(r));
  }

  @Transactional(readOnly = true)
  public Long resolveManagerHeadquarter(JwtAuthenticationContext principal) {
    List<Long> ids = assignedHeadquarters(principal);
    if (ids.size() == 1) {
      return ids.getFirst();
    }
    if (ids.isEmpty()) {
      throw new ForbiddenException(
          "Manager has no assigned headquarter",
          Map.of("userId", principal.userId()),
          "MANAGER without account_user_headquarters row");
    }
    throw new ForbiddenException(
        "Manager must have exactly one assigned headquarter for this operation",
        Map.of("userId", principal.userId(), "assignedCount", ids.size()),
        "MANAGER with multiple HQs");
  }

  @Transactional(readOnly = true)
  public Long enforceHeadquarterFilter(JwtAuthenticationContext principal, Long requestedHeadquarterId) {
    if (isAdmin(principal)) {
      return requestedHeadquarterId;
    }
    if (isManager(principal)) {
      Long managerHq = resolveManagerHeadquarter(principal);
      if (requestedHeadquarterId != null && !requestedHeadquarterId.equals(managerHq)) {
        throw forbidden(requestedHeadquarterId);
      }
      return managerHq;
    }
    if (requestedHeadquarterId != null) {
      requireHeadquarterAccess(principal, requestedHeadquarterId);
    }
    return requestedHeadquarterId;
  }

  /** Returns all HQs in scope; null denotes an ADMIN-wide query. */
  @Transactional(readOnly = true)
  public List<Long> enforceHeadquarterScope(JwtAuthenticationContext principal, Long requestedHeadquarterId) {
    if (isAdmin(principal)) {
      return requestedHeadquarterId == null ? null : List.of(requestedHeadquarterId);
    }
    if (requestedHeadquarterId != null) {
      requireHeadquarterAccess(principal, requestedHeadquarterId);
      return List.of(requestedHeadquarterId);
    }
    return isScopedStaff(principal) ? List.copyOf(assignedHeadquarters(principal)) : List.of();
  }

  private boolean isScopedStaff(JwtAuthenticationContext principal) {
    return isManager(principal)
        || principal.roles().stream()
            .anyMatch(r -> "POS_OPERATOR".equals(r) || "ROLE_POS_OPERATOR".equals(r));
  }

  /** Null-owned warehouse locations are global data and are ADMIN-only. */
  @Transactional(readOnly = true)
  public void requireOwnedLocation(JwtAuthenticationContext principal, Long headquarterId) {
    if (headquarterId == null) {
      requireGlobalPosAccess(principal);
    } else {
      requireHeadquarterAccess(principal, headquarterId);
    }
  }

  private User loadUser(JwtAuthenticationContext principal) {
    return userRepository
        .findByIdAndDeletedAtIsNull(principal.userId())
        .orElseThrow(() -> new UserNotFoundException(principal.userId()));
  }

  private static ForbiddenException forbidden(long headquarterId) {
    return new ForbiddenException(
        "Access denied for this headquarter",
        Map.of("headquarterId", headquarterId),
        "headquarter access denied");
  }
}
