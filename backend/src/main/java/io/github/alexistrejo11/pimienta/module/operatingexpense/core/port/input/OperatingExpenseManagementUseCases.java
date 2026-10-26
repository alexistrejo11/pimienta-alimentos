package io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.input;

import io.github.alexistrejo11.pimienta.config.security.JwtAuthenticationContext;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command.CreateOperatingExpenseCommand;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.command.UpdateOperatingExpenseCommand;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseListQuery;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSummary;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OperatingExpenseManagementUseCases {

  Page<OperatingExpense> search(JwtAuthenticationContext principal, OperatingExpenseListQuery query, Pageable pageable);

  OperatingExpenseSummary summarize(JwtAuthenticationContext principal, OperatingExpenseListQuery query);

  OperatingExpense getById(JwtAuthenticationContext principal, long id);

  OperatingExpense create(JwtAuthenticationContext principal, CreateOperatingExpenseCommand command);

  OperatingExpense update(JwtAuthenticationContext principal, long id, UpdateOperatingExpenseCommand command);

  void delete(JwtAuthenticationContext principal, long id);
}
