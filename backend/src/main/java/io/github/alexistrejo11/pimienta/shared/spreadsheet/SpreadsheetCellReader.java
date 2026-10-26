package io.github.alexistrejo11.pimienta.shared.spreadsheet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Date1904Support;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

/** Lectura de celdas .xlsx sin depender de un módulo de dominio concreto. */
public final class SpreadsheetCellReader {

  private static final DataFormatter FORMATTER = new DataFormatter();

  private SpreadsheetCellReader() {}

  public static String stringAt(Row row, Integer colIdx) {
    if (colIdx == null) {
      return "";
    }
    Cell cell = row.getCell(colIdx);
    return cell == null ? "" : FORMATTER.formatCellValue(cell).trim();
  }

  public static Long longAt(Row row, Integer colIdx) {
    if (colIdx == null) {
      return null;
    }
    Cell cell = row.getCell(colIdx);
    if (cell == null) {
      return null;
    }
    try {
      CellType type = cell.getCellType();
      if (type == CellType.FORMULA) {
        type = cell.getCachedFormulaResultType();
      }
      return switch (type) {
        case NUMERIC -> Math.round(cell.getNumericCellValue());
        case STRING -> {
          String s = cell.getStringCellValue().trim();
          if (s.isEmpty() || "-".equals(s)) {
            yield null;
          }
          yield Long.parseLong(s);
        }
        case BLANK -> null;
        default -> {
          String t = FORMATTER.formatCellValue(cell).trim();
          if (t.isEmpty() || "-".equals(t)) {
            yield null;
          }
          yield Long.parseLong(t);
        }
      };
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("ID inválido en fila " + (row.getRowNum() + 1));
    }
  }

  public static BigDecimal bigDecimalAt(Row row, Integer colIdx) {
    String s = stringAt(row, colIdx);
    if (s.isEmpty()) {
      return null;
    }
    try {
      return new BigDecimal(s.replace(',', '.'));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static LocalDate localDateAt(Row row, Integer colIdx) {
    if (colIdx == null) {
      return null;
    }
    Cell cell = row.getCell(colIdx);
    if (cell == null) {
      return null;
    }
    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      double serial = cell.getNumericCellValue();
      if (isCalendarDateWithoutTime(serial)) {
        boolean use1904 =
            cell.getSheet().getWorkbook() instanceof Date1904Support dated && dated.isDate1904();
        return DateUtil.getLocalDateTime(serial, use1904).toLocalDate();
      }
      return cell.getDateCellValue().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }
    String s = FORMATTER.formatCellValue(cell).trim();
    if (s.isEmpty()) {
      return null;
    }
    try {
      return LocalDate.parse(s);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private static boolean isCalendarDateWithoutTime(double serial) {
    double fraction = serial - Math.floor(serial);
    return fraction < (0.5d / 86400d);
  }

  public static boolean isBlankRow(String... values) {
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return false;
      }
    }
    return true;
  }

  public static String normalizeHeader(String h) {
    return h.trim().toLowerCase(Locale.ROOT);
  }
}
