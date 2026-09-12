package io.github.alexistrejo11.pimienta.module.payroll.core.application;

import io.github.alexistrejo11.pimienta.module.payroll.core.application.query.PayrollBulkScopeQuery;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.data.domain.Pageable;

public interface PayrollBulkSyncUseCases {

  byte[] exportPayrollRecords(PayrollBulkScopeQuery scope, Pageable pageable) throws IOException;

  SpreadsheetBulkImportResult importPayrollRecords(
      InputStream file, String originalFilename, PayrollBulkScopeQuery scope, boolean dryRun)
      throws IOException;
}
