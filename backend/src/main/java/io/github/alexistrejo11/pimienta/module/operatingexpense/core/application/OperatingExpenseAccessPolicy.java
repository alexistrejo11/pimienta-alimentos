package io.github.alexistrejo11.pimienta.module.operatingexpense.core.application;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.config.web.ClientTimeZones;
import io.github.alexistrejo11.pimienta.module.account.user.core.application.HeadquarterAccessService;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseListQuery;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSearchCriteria;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import io.github.alexistrejo11.pimienta.shared.exception.BusinessValidationException;
import io.github.alexistrejo11.pimienta.shared.exception.ForbiddenException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * OperatingExpense rules only. {@link HeadquarterAccessService} is used to read assigned sedes and is not
 * changed: DIRECTOR stays sede-scoped everywhere else.
 */
@Component
public class OperatingExpenseAccessPolicy {

  private final HeadquarterAccessService headquarterAccess;

  @Autowired
  public OperatingExpenseAccessPolicy(HeadquarterAccessService headquarterAccess) {
    this.headquarterAccess = headquarterAccess;
  }

  public LocalDate today() {
    return LocalDate.now(ClientTimeZones.current());
  }

  public OperatingExpenseSearchCriteria resolveList(JwtAuthenticationContext principal, OperatingExpenseListQuery query) {
    requireReader(principal);
    OperatingExpenseListQuery effective =
        query != null ? query : new OperatingExpenseListQuery(null, null, null, null, null);
    if (unrestricted(principal)) {
      return new OperatingExpenseSearchCriteria(
          effective.search(), effective.headquarterId(), effective.from(), effective.to(), effective.category());
    }
    LocalDate today = today();
    if (effective.from() != null && !effective.from().equals(today)) {
      throw dateForbidden(principal);
    }
    if (effective.to() != null && !effective.to().equals(today)) {
      throw dateForbidden(principal);
    }
    long headquarterId = resolveAssignedHeadquarter(principal, effective.headquarterId());
    return new OperatingExpenseSearchCriteria(
        effective.search(), headquarterId, today, today, effective.category());
  }

  public long resolveWriteHeadquarter(JwtAuthenticationContext principal, Long requested) {
    if (unrestricted(principal)) {
      if (requested == null) {
        throw new BusinessValidationException(
            "La sede es obligatoria.",
            Map.of(),
            "expense headquarter is required for unrestricted roles");
      }
      return requested;
    }
    return resolveAssignedHeadquarter(principal, requested);
  }

  public LocalDate resolveOccurredOn(JwtAuthenticationContext principal, LocalDate requested) {
    LocalDate today = today();
    if (unrestricted(principal)) {
      return requested != null ? requested : today;
    }
    requireReader(principal);
    if (requested != null && !requested.equals(today)) {
      throw dateForbidden(principal);
    }
    return today;
  }

  public void requireVisible(JwtAuthenticationContext principal, OperatingExpense expense) {
    requireReader(principal);
    if (unrestricted(principal)) {
      return;
    }
    List<Long> assigned = headquarterAccess.assignedHeadquarters(principal);
    if (!assigned.contains(expense.getHeadquarterId())) {
      throw sedeForbidden(principal, expense.getHeadquarterId());
    }
    if (expense.getOccurredOn() == null || !expense.getOccurredOn().equals(today())) {
      throw dateForbidden(principal);
    }
  }

  public void requireMaintainer(JwtAuthenticationContext principal) {
    if (!unrestricted(principal)) {
      throw new ForbiddenException(
          "Solo administración y dirección pueden modificar gastos operativos.",
          Map.of("userId", principal.userId()),
          "expense update/delete denied");
    }
  }

  private long resolveAssignedHeadquarter(JwtAuthenticationContext principal, Long requested) {
    requireReader(principal);
    List<Long> assigned = headquarterAccess.assignedHeadquarters(principal);
    if (assigned.isEmpty()) {
      throw new ForbiddenException(
          "No tienes una sede asignada.",
          Map.of("userId", principal.userId()),
          "expense staff without assigned headquarter");
    }
    if (requested == null) {
      if (assigned.size() == 1) {
        return assigned.getFirst();
      }
      throw new ForbiddenException(
          "Indica una de tus sedes asignadas.",
          Map.of("userId", principal.userId(), "assignedCount", assigned.size()),
          "expense staff with multiple headquarters omitted headquarterId");
    }
    if (!assigned.contains(requested)) {
      throw sedeForbidden(principal, requested);
    }
    return requested;
  }

  private void requireReader(JwtAuthenticationContext principal) {
    if (unrestricted(principal) || isManager(principal) || isEmployee(principal)) {
      return;
    }
    throw new ForbiddenException(
        "No tienes acceso a gastos operativos.",
        Map.of("userId", principal.userId()),
        "expense access denied for role");
  }

  private boolean unrestricted(JwtAuthenticationContext principal) {
    return headquarterAccess.isAdmin(principal) || headquarterAccess.isDirector(principal);
  }

  private boolean isManager(JwtAuthenticationContext principal) {
    return headquarterAccess.isManager(principal);
  }

  private boolean isEmployee(JwtAuthenticationContext principal) {
    return hasRole(principal, "EMPLOYEE");
  }

  private static boolean hasRole(JwtAuthenticationContext principal, String role) {
    return principal.roles().stream().anyMatch(r -> role.equals(r) || ("ROLE_" + role).equals(r));
  }

  private static ForbiddenException dateForbidden(JwtAuthenticationContext principal) {
    return new ForbiddenException(
        "Solo puedes ver y registrar gastos operativos de hoy.",
        Map.of("userId", principal.userId()),
        "expense date outside today");
  }

  private static ForbiddenException sedeForbidden(JwtAuthenticationContext principal, long headquarterId) {
    return new ForbiddenException(
        "No puedes consultar gastos operativos de esta sede.",
        Map.of("userId", principal.userId(), "headquarterId", headquarterId),
        "expense headquarter denied");
  }
}
