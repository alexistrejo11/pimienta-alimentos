package io.github.alexistrejo11.pimienta.shared.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Locale;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * H2 tests do not run Flyway. Hibernate {@code create-drop} still has to choose {@code timestamptz}
 * for every {@code LocalDateTime} and {@code Instant} column, which is what {@code ddl-auto:
 * validate} compares on PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class UtcLocalDateTimeConverterSchemaTest {

  @Autowired private DataSource dataSource;

  @Test
  void localDateTimeAndInstantColumnsAreTimestampWithTimeZone() throws Exception {
    assertTimestampWithTimeZone("headquarters", "created_at");
    assertTimestampWithTimeZone("employee_attendances", "check_in_time");
    assertTimestampWithTimeZone("pos_sales", "occurred_at");
    assertTimestampWithTimeZone("tasks", "due_date");
  }

  private void assertTimestampWithTimeZone(String table, String column) throws Exception {
    String typeName = columnType(table, column);
    assertThat(typeName)
        .as("%s.%s", table, column)
        .isNotNull()
        .containsIgnoringCase("WITH TIME ZONE");
  }

  private String columnType(String table, String column) throws Exception {
    try (Connection connection = dataSource.getConnection();
        ResultSet columns =
            connection.getMetaData().getColumns(null, null, table.toUpperCase(Locale.ROOT), column.toUpperCase(Locale.ROOT))) {
      if (columns.next()) {
        return columns.getString("TYPE_NAME");
      }
    }
    try (Connection connection = dataSource.getConnection();
        ResultSet columns = connection.getMetaData().getColumns(null, null, table, column)) {
      if (columns.next()) {
        return columns.getString("TYPE_NAME");
      }
    }
    return null;
  }
}
