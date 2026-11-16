package io.github.alexistrejo11.pimienta.module.crm.adapter.out.excel;

import io.github.alexistrejo11.pimienta.module.crm.core.application.dto.OpportunityExportRow;
import io.github.alexistrejo11.pimienta.module.crm.core.port.OpportunitySpreadsheetGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import io.github.alexistrejo11.pimienta.shared.util.Strings;

@Component
public class OpportunitySpreadsheetGeneratorImpl implements OpportunitySpreadsheetGenerator {

  @Override
  public byte[] generate(List<OpportunityExportRow> rows) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Opportunities");
      Row header = sheet.createRow(0);
      int hc = 0;
      for (OpportunitySpreadsheetColumn col : OpportunitySpreadsheetColumn.values()) {
        header.createCell(hc++).setCellValue(col.header());
      }
      int r = 1;
      for (OpportunityExportRow row : rows) {
        Row dataRow = sheet.createRow(r++);
        int c = 0;
        if (row.id() != null) {
          dataRow.createCell(c++).setCellValue(row.id());
        } else {
          dataRow.createCell(c++).setCellValue("");
        }
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.title()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.description()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.contactName()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.contactEmail()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.contactPhone()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.companyName()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.companyLocation()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.industry()));
        dataRow
            .createCell(c++)
            .setCellValue(row.estimatedValue() != null ? row.estimatedValue().doubleValue() : 0);
        dataRow.createCell(c++).setCellValue(row.probabilityPercent());
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.source()));
        if (row.expectedCloseDate() != null) {
          dataRow.createCell(c++).setCellValue(row.expectedCloseDate().toString());
        } else {
          dataRow.createCell(c++).setCellValue("");
        }
        if (row.assignedSalesmanId() != null) {
          dataRow.createCell(c++).setCellValue(row.assignedSalesmanId());
        } else {
          dataRow.createCell(c++).setCellValue("");
        }
        dataRow.createCell(c).setCellValue(Strings.nullToEmpty(row.status()));
      }
      sheet.setColumnHidden(0, true);
      workbook.write(out);
      return out.toByteArray();
    }
  }

}
