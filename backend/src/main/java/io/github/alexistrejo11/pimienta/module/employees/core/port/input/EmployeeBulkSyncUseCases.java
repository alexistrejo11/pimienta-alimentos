package io.github.alexistrejo11.pimienta.module.employees.core.port.input;

import io.github.alexistrejo11.pimienta.module.employees.core.application.query.EmployeeSearchCriteria;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.data.domain.Pageable;

public interface EmployeeBulkSyncUseCases {

  byte[] exportEmployees(EmployeeSearchCriteria criteria, Pageable pageable) throws IOException;

  SpreadsheetBulkImportResult importEmployees(
      InputStream file, String originalFilename, boolean dryRun)
      throws IOException;
}
