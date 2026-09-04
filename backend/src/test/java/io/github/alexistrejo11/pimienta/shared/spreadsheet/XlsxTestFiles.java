package io.github.alexistrejo11.pimienta.shared.spreadsheet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;

/** Builds in-memory .xlsx files for import integration tests. */
public final class XlsxTestFiles {

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private XlsxTestFiles() {}

  public static MockMultipartFile multipart(String filename, String[] headers, Object[]... dataRows)
      throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("sheet");
      Row header = sheet.createRow(0);
      for (int c = 0; c < headers.length; c++) {
        header.createCell(c).setCellValue(headers[c]);
      }
      for (int r = 0; r < dataRows.length; r++) {
        Row row = sheet.createRow(r + 1);
        Object[] values = dataRows[r];
        for (int c = 0; c < values.length; c++) {
          setCell(row, c, values[c]);
        }
      }
      workbook.write(out);
      return new MockMultipartFile("file", filename, XLSX, out.toByteArray());
    }
  }

  private static void setCell(Row row, int col, Object value) {
    if (value == null) {
      return;
    }
    if (value instanceof Number n) {
      row.createCell(col).setCellValue(n.doubleValue());
      return;
    }
    if (value instanceof BigDecimal bd) {
      row.createCell(col).setCellValue(bd.doubleValue());
      return;
    }
    if (value instanceof LocalDate d) {
      row.createCell(col).setCellValue(d.toString());
      return;
    }
    row.createCell(col).setCellValue(value.toString());
  }
}
