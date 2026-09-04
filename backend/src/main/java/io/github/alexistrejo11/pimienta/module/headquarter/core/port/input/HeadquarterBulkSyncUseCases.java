package io.github.alexistrejo11.pimienta.module.headquarter.core.port.input;

import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.data.domain.Pageable;

public interface HeadquarterBulkSyncUseCases {

  byte[] exportHeadquarters(Pageable pageable) throws IOException;

  SpreadsheetBulkImportResult importHeadquarters(
      InputStream file, String originalFilename, boolean dryRun)
      throws IOException;
}
