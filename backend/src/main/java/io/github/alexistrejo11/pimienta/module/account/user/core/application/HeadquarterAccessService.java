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

/** Enforces headquarter scope for sede staff. ADMIN bypasses checks. */
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

  /** Scoped staff must pass an assigned HQ; ADMIN may omit for global views. */
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

  /** Sales, product mix, shift closes, and cash reconciliation. */
  @Transactional(readOnly = true)
  public boolean canViewPosFinancials(JwtAuthenticationContext principal) {
    return isAdmin(principal) || isDirector(principal);
  }

  @Transactional(readOnly = true)
  public void requirePosFinancialAccess(JwtAuthenticationContext principal) {
    if (canViewPosFinancials(principal)) {
      return;
    }
    throw financialForbidden(principal);
  }

  /** Floor staff may inspect open shifts only. */
  @Transactional(readOnly = true)
  public void requireOpenShift(JwtAuthenticationContext principal, String status) {
    if (canViewPosFinancials(principal) || "OPEN".equals(status)) {
      return;
    }
    throw financialForbidden(principal);
  }

  @Transactional(readOnly = true)
  public List<Long> assignedHeadquarters(JwtAuthenticationContext principal) {
    return loadUser(principal).getAssignedHeadquarterIds();
  }

  @Transactional(readOnly = true)
  public boolean isAdmin(JwtAuthenticationContext principal) {
    return hasRole(principal, "ADMIN");
  }

  @Transactional(readOnly = true)
  public boolean isDirector(JwtAuthenticationContext principal) {
    return hasRole(principal, "DIRECTOR");
  }

  @Transactional(readOnly = true)
  public boolean isManager(JwtAuthenticationContext principal) {
    return hasRole(principal, "MANAGER");
  }

  @Transactional(readOnly = true)
  public Long resolveManagerHeadquarter(JwtAuthenticationContext principal) {
    List<Long> ids = assignedHeadquarters(principal);
    if (ids.size() == 1) {
      return ids.getFirst();
    }
    if (ids.isEmpty()) {
      throw new ForbiddenException(
          "Assigned headquarter is required",
          Map.of("userId", principal.userId()),
          "scoped staff without account_user_headquarters row");
    }
    throw new ForbiddenException(
        "Exactly one assigned headquarter is required for this operation",
        Map.of("userId", principal.userId(), "assignedCount", ids.size()),
        "scoped staff with multiple HQs");
  }

  @Transactional(readOnly = true)
  public Long enforceHeadquarterFilter(JwtAuthenticationContext principal, Long requestedHeadquarterId) {
    if (isAdmin(principal)) {
      return requestedHeadquarterId;
    }
    if (isScopedStaff(principal)) {
      Long assigned = resolveManagerHeadquarter(principal);
      if (requestedHeadquarterId != null && !requestedHeadquarterId.equals(assigned)) {
        throw forbidden(requestedHeadquarterId);
      }
      return assigned;
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
    return isDirector(principal) || isManager(principal) || hasRole(principal, "EMPLOYEE");
  }

  private static boolean hasRole(JwtAuthenticationContext principal, String role) {
    return principal.roles().stream().anyMatch(r -> role.equals(r) || ("ROLE_" + role).equals(r));
  }

  private static ForbiddenException financialForbidden(JwtAuthenticationContext principal) {
    return new ForbiddenException(
        "No tienes permiso para ver ventas ni cortes",
        Map.of("userId", principal.userId()),
        "pos financial access denied");
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
