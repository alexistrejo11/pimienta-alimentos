package io.github.alexistrejo11.pimienta.module.headquarter.core.application;

import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.CreateHeadquarterCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.command.UpdateHeadquarterCommand;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.dto.HeadquarterExportRow;
import io.github.alexistrejo11.pimienta.module.headquarter.core.application.dto.HeadquarterImportRow;
import io.github.alexistrejo11.pimienta.module.headquarter.core.domain.Headquarter;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterBulkSyncUseCases;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterSpreadsheetGenerator;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterSpreadsheetParser;
import io.github.alexistrejo11.pimienta.module.headquarter.core.port.input.HeadquarterUseCases;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportRowError;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HeadquarterBulkSyncUseCasesImpl implements HeadquarterBulkSyncUseCases {

  private static final int PAGE_SIZE = 500;

  private final HeadquarterUseCases headquarterUseCases;
  private final HeadquarterSpreadsheetParser spreadsheetParser;
  private final HeadquarterSpreadsheetGenerator spreadsheetGenerator;

  public HeadquarterBulkSyncUseCasesImpl(
      HeadquarterUseCases headquarterUseCases,
      HeadquarterSpreadsheetParser spreadsheetParser,
      HeadquarterSpreadsheetGenerator spreadsheetGenerator) {
    this.headquarterUseCases = headquarterUseCases;
    this.spreadsheetParser = spreadsheetParser;
    this.spreadsheetGenerator = spreadsheetGenerator;
  }

  @Override
  public byte[] exportHeadquarters(Pageable pageable) throws IOException {
    List<HeadquarterExportRow> rows = new ArrayList<>();
    int page = 0;
    Sort sort = pageable != null && pageable.getSort().isSorted()
        ? pageable.getSort()
        : Sort.by(Sort.Direction.ASC, "id");
    for (;;) {
      Page<Headquarter> p = headquarterUseCases.getBy(PageRequest.of(page, PAGE_SIZE, sort));
      for (Headquarter h : p.getContent()) {
        rows.add(
            new HeadquarterExportRow(
                h.getId(), h.getName(), h.getAddress(), h.getDescription()));
      }
      if (!p.hasNext()) {
        break;
      }
      page++;
    }
    return spreadsheetGenerator.generate(rows);
  }

  @Override
  @Transactional
  public SpreadsheetBulkImportResult importHeadquarters(
      InputStream file, String originalFilename, boolean dryRun)
      throws IOException {
    List<HeadquarterImportRow> parsed = spreadsheetParser.parse(file, originalFilename);
    List<SpreadsheetBulkImportRowError> errors = new ArrayList<>();
    List<Runnable> applies = new ArrayList<>();
    int skipped = 0;
    int updated = 0;
    int created = 0;

    for (HeadquarterImportRow row : parsed) {
      try {
        if (row.id() == null && (row.name() == null || row.name().isBlank())) {
          skipped++;
          continue;
        }
        if (row.id() != null) {
          Headquarter existing = headquarterUseCases.getById(row.id());
          String name =
              row.name() == null || row.name().isBlank() ? existing.getName() : row.name().trim();
          String address =
              row.address() == null || row.address().isBlank()
                  ? existing.getAddress()
                  : row.address().trim();
          String description =
              row.description() == null || row.description().isBlank()
                  ? existing.getDescription()
                  : row.description().trim();
          Long id = row.id();
          applies.add(
              () ->
                  headquarterUseCases.update(
                      id, new UpdateHeadquarterCommand(name, address, description)));
          updated++;
        } else {
          if (row.name() == null || row.name().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio para altas");
          }
          if (row.address() == null || row.address().isBlank()) {
            throw new IllegalArgumentException("La dirección es obligatoria para altas");
          }
          if (row.description() == null || row.description().isBlank()) {
            throw new IllegalArgumentException("La descripción es obligatoria para altas");
          }
          applies.add(
              () ->
                  headquarterUseCases.create(
                      new CreateHeadquarterCommand(
                          row.name().trim(), row.address().trim(), row.description().trim())));
          created++;
        }
      } catch (Exception ex) {
        errors.add(
            new SpreadsheetBulkImportRowError(
                row.excelRowNumber(),
                ex.getMessage() != null ? ex.getMessage() : "Error al procesar fila"));
      }
    }

    if (!errors.isEmpty()) {
      return new SpreadsheetBulkImportResult(0, 0, skipped, List.copyOf(errors), dryRun);
    }
    if (!dryRun) {
      for (Runnable apply : applies) {
        apply.run();
      }
    }
    return new SpreadsheetBulkImportResult(updated, created, skipped, List.of(), dryRun);
  }
}
