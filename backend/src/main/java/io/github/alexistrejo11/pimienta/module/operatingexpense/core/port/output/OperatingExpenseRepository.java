package io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseCategoryTotal;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseDayTotal;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSearchCriteria;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OperatingExpenseRepository {

  Optional<OperatingExpense> findById(long id);

  Page<OperatingExpense> search(OperatingExpenseSearchCriteria criteria, Pageable pageable);

  BigDecimal sumAmount(OperatingExpenseSearchCriteria criteria);

  /** Days with expenses, ordered by occurredOn ascending. Uses the same filters as {@link #sumAmount}. */
  List<OperatingExpenseDayTotal> sumByDay(OperatingExpenseSearchCriteria criteria);

  /** Category totals for the same filters as {@link #sumAmount}. */
  List<OperatingExpenseCategoryTotal> sumByCategory(OperatingExpenseSearchCriteria criteria);

  OperatingExpense save(OperatingExpense expense);
}
