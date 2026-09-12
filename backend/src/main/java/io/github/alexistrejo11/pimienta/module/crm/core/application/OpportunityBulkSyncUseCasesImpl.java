package io.github.alexistrejo11.pimienta.module.crm.core.application;

import io.github.alexistrejo11.pimienta.module.crm.core.application.command.CreateOpportunityParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.command.UpdateOpportunityParams;
import io.github.alexistrejo11.pimienta.module.crm.core.application.dto.OpportunityExportRow;
import io.github.alexistrejo11.pimienta.module.crm.core.application.dto.OpportunityImportRow;
import io.github.alexistrejo11.pimienta.module.crm.core.application.query.OpportunitySearchCriteria;
import io.github.alexistrejo11.pimienta.module.crm.core.domain.Opportunity;
import io.github.alexistrejo11.pimienta.module.crm.core.port.input.OpportunityUseCases;
import io.github.alexistrejo11.pimienta.module.crm.core.port.output.OpportunityRepository;
import io.github.alexistrejo11.pimienta.module.crm.core.port.OpportunitySpreadsheetGenerator;
import io.github.alexistrejo11.pimienta.module.crm.core.port.OpportunitySpreadsheetParser;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportResult;
import io.github.alexistrejo11.pimienta.shared.spreadsheet.SpreadsheetBulkImportRowError;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityBulkSyncUseCasesImpl implements OpportunityBulkSyncUseCases {

  private static final Logger log = LoggerFactory.getLogger(OpportunityBulkSyncUseCasesImpl.class);

  private static final int PAGE_SIZE = 500;

  private final OpportunityRepository opportunityRepository;
  private final OpportunityUseCases opportunityUseCases;
  private final OpportunitySpreadsheetParser spreadsheetParser;
  private final OpportunitySpreadsheetGenerator spreadsheetGenerator;

  public OpportunityBulkSyncUseCasesImpl(
      OpportunityRepository opportunityRepository,
      OpportunityUseCases opportunityUseCases,
      OpportunitySpreadsheetParser spreadsheetParser,
      OpportunitySpreadsheetGenerator spreadsheetGenerator) {
    this.opportunityRepository = opportunityRepository;
    this.opportunityUseCases = opportunityUseCases;
    this.spreadsheetParser = spreadsheetParser;
    this.spreadsheetGenerator = spreadsheetGenerator;
  }

  @Override
  public byte[] exportOpportunities(OpportunitySearchCriteria criteria, Pageable pageable)
      throws IOException {
    OpportunitySearchCriteria effective =
        criteria != null ? criteria : OpportunitySearchCriteria.empty();

    log.debug(
        "export opportunities start status={} companyNameFilterLen={} titleFilterLen={}",
        effective.status(),
        effective.companyNameContains() != null ? effective.companyNameContains().length() : 0,
        effective.titleContains() != null ? effective.titleContains().length() : 0);

    List<OpportunityExportRow> rows = new ArrayList<>();
    int page = 0;
    Sort sort =
        pageable != null && pageable.getSort().isSorted()
            ? pageable.getSort()
            : Sort.by(Sort.Direction.ASC, "id");

    for (;;) {
      Page<Opportunity> p =
          opportunityRepository.search(effective, PageRequest.of(page, PAGE_SIZE, sort));

      for (Opportunity o : p.getContent()) {
        rows.add(toExportRow(o));
      }
      if (!p.hasNext()) {
        break;
      }
      page++;
    }

    byte[] bytes = spreadsheetGenerator.generate(rows);

    log.debug("export opportunities complete rowCount={} bytesLen={}", rows.size(), bytes.length);
    return bytes;
  }

  private static OpportunityExportRow toExportRow(Opportunity o) {
    return new OpportunityExportRow(
        o.getId(),
        o.getTitle(),
        o.getDescription(),
        o.getContactName(),
        o.getContactEmail(),
        o.getContactPhone(),
        o.getCompanyName(),
        o.getCompanyLocation(),
        o.getIndustry(),
        o.getEstimatedValue(),
        o.getProbabilityPercent(),
        o.getSource() != null ? o.getSource().name() : Opportunity.OpportunitySource.OTHER.name(),
        o.getExpectedCloseDate(),
        o.getAssignedSalesmanId(),
        o.getStatus().name());
  }

  @Override
  @Transactional
  public SpreadsheetBulkImportResult importOpportunities(
      InputStream file, String originalFilename, boolean dryRun)
      throws IOException {
    log.info(
        "import opportunities start dryRun={} originalFilename={}",
        dryRun,
        originalFilename != null ? originalFilename : "null");

    List<OpportunityImportRow> parsed = spreadsheetParser.parse(file, originalFilename);
    List<SpreadsheetBulkImportRowError> errors = new ArrayList<>();
    List<Runnable> applies = new ArrayList<>();
    int skipped = 0;
    int updated = 0;
    int created = 0;

    for (OpportunityImportRow row : parsed) {
      try {
        if (row.id() == null && (row.title() == null || row.title().isBlank())) {
          skipped++;
          continue;
        }
        if (row.id() != null) {
          Opportunity existing = opportunityRepository.findById(row.id()).orElse(null);
          if (existing == null) {
            throw new IllegalArgumentException("No existe oportunidad con ID " + row.id());
          }
          UpdateOpportunityParams merged = merge(row, existing);
          Long id = row.id();
          applies.add(() -> opportunityUseCases.update(id, merged));
          updated++;
        } else {
          if (row.title() == null || row.title().isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio para altas");
          }
          CreateOpportunityParams cp =
              new CreateOpportunityParams(
                  row.title().trim(),
                  pOrEmpty(row.description()),
                  pOrEmpty(row.contactName()),
                  pOrEmpty(row.contactEmail()),
                  pOrEmpty(row.contactPhone()),
                  pOrEmpty(row.companyName()),
                  pOrEmpty(row.companyLocation()),
                  pOrEmpty(row.industry()),
                  row.estimatedValue() != null ? row.estimatedValue() : BigDecimal.ZERO,
                  row.probabilityPercent() != null ? row.probabilityPercent() : 0,
                  parseSourceSafe(row.sourceRaw()),
                  row.expectedCloseDate(),
                  row.assignedSalesmanId());
          applies.add(() -> opportunityUseCases.create(cp));
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

  private static UpdateOpportunityParams merge(OpportunityImportRow row, Opportunity o) {
    return new UpdateOpportunityParams(
        pOrNull(row.title()),
        pOrNull(row.description()),
        pOrNull(row.contactName()),
        pOrNull(row.contactEmail()),
        pOrNull(row.contactPhone()),
        pOrNull(row.companyName()),
        pOrNull(row.companyLocation()),
        pOrNull(row.industry()),
        row.estimatedValue() != null ? row.estimatedValue() : null,
        row.probabilityPercent() != null ? row.probabilityPercent() : null,
        pOrNull(row.sourceRaw()) != null ? parseSourceSafe(row.sourceRaw()) : null,
        row.expectedCloseDate() != null ? row.expectedCloseDate() : null,
        row.assignedSalesmanId() != null ? row.assignedSalesmanId() : null);
  }

  private static String pOrNull(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    return s.trim();
  }

  private static String pOrEmpty(String s) {
    if (s == null || s.isBlank()) {
      return "";
    }
    return s.trim();
  }

  private static Opportunity.OpportunitySource parseSourceSafe(String raw) {
    if (raw == null || raw.isBlank()) {
      return Opportunity.OpportunitySource.OTHER;
    }
    String s =
        raw.trim()
            .toUpperCase(Locale.ROOT)
            .replace(' ', '_')
            .replace('-', '_');
    return Opportunity.OpportunitySource.valueOf(s);
  }
}
