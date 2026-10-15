package io.github.alexistrejo11.pimienta.module.operatingexpense.core.port.output;

import io.github.alexistrejo11.pimienta.module.operatingexpense.core.application.query.OperatingExpenseSearchCriteria;
import io.github.alexistrejo11.pimienta.module.operatingexpense.core.domain.OperatingExpense;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OperatingExpenseRepository {

  Optional<OperatingExpense> findById(long id);

  Page<OperatingExpense> search(OperatingExpenseSearchCriteria criteria, Pageable pageable);

  BigDecimal sumAmount(OperatingExpenseSearchCriteria criteria);

  OperatingExpense save(OperatingExpense expense);
}
