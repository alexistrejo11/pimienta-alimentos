package io.github.alexistrejo11.pimienta.module.employees.adapter.outbound.excel;

import io.github.alexistrejo11.pimienta.module.employees.core.application.dto.EmployeeExportRow;
import io.github.alexistrejo11.pimienta.module.employees.core.port.input.EmployeeSpreadsheetGenerator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import io.github.alexistrejo11.pimienta.shared.util.Strings;

@Component
public class EmployeeSpreadsheetGeneratorImpl implements EmployeeSpreadsheetGenerator {

  @Override
  public byte[] generate(List<EmployeeExportRow> rows) throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Employees");
      Row header = sheet.createRow(0);
      int hc = 0;
      for (EmployeeSpreadsheetColumn col : EmployeeSpreadsheetColumn.values()) {
        header.createCell(hc++).setCellValue(col.header());
      }
      int r = 1;
      for (EmployeeExportRow row : rows) {
        Row dataRow = sheet.createRow(r++);
        int c = 0;
        if (row.id() != null) {
          dataRow.createCell(c++).setCellValue(row.id());
        } else {
          dataRow.createCell(c++).setCellValue("");
        }
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.firstName()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.lastName()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.email()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.phone()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.address()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.curp()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.rfc()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.nss()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.clabe()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.position()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.department()));
        dataRow
            .createCell(c++)
            .setCellValue(row.status() != null ? row.status().name() : "");
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.contractType()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.workShift()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.salaryPerWeek()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.bonuses()));
        dataRow.createCell(c++).setCellValue(Strings.nullToEmpty(row.foodVouchers()));
        dataRow.createCell(c).setCellValue(Strings.nullToEmpty(row.integrationFactor()));
      }
      sheet.setColumnHidden(0, true);
      workbook.write(out);
      return out.toByteArray();
    }
  }

}
