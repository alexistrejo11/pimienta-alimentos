package io.github.alexistrejo11.pimienta.module.employees.core.port.input;

import io.github.alexistrejo11.pimienta.module.employees.core.application.command.ChangeEmployeeStatusCommand;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.RegisterEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.param.UpdateEmployeeParams;
import io.github.alexistrejo11.pimienta.module.employees.core.application.query.EmployeeSearchCriteria;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeStatistics;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.EmployeeSummary;
import io.github.alexistrejo11.pimienta.module.employees.core.domain.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EmployeeManagementUseCases {

  EmployeeStatistics statistics();

  EmployeeSummary summary();

  Page<Employee> search(EmployeeSearchCriteria criteria, Pageable pageable);

  Employee getById(Long id);

  Employee register(RegisterEmployeeParams params);

  Employee update(Long id, UpdateEmployeeParams params);

  /** Direct lifecycle status mutation (ACTIVE, INACTIVE, RESIGNED, TERMINATED, DOWN). */
  Employee changeStatus(ChangeEmployeeStatusCommand command);

  void delete(Long id);
}
