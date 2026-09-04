package io.github.alexistrejo11.pimienta.module.crm.core.application;

import io.github.alexistrejo11.pimienta.module.crm.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.data.domain.Pageable;

public interface OpportunityBulkSyncUseCases {

  byte[] exportOpportunities(OpportunitySearchCriteria criteria, Pageable pageable) throws IOException;

  SpreadsheetBulkImportResult importOpportunities(
      InputStream file, String originalFilename, boolean dryRun)
      throws IOException;
}
