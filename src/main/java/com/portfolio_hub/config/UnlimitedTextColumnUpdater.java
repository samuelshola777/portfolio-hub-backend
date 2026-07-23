package com.portfolio_hub.config;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Existing installations may still have VARCHAR(n) columns created by older
 * entity mappings. Convert them to PostgreSQL TEXT after Hibernate has applied
 * its schema updates so removed request limits are also removed at the database
 * boundary. The migration is idempotent and deliberately PostgreSQL-only.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class UnlimitedTextColumnUpdater implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    if (!databaseProduct().toLowerCase().contains("postgresql")) return;

    List<Map<String, Object>> columns = jdbcTemplate.queryForList(
      """
      SELECT table_name, column_name
      FROM information_schema.columns
      WHERE table_schema = current_schema()
        AND data_type = 'character varying'
      ORDER BY table_name, ordinal_position
      """
    );

    for (Map<String, Object> column : columns) {
      String tableName = identifier(column.get("table_name"));
      String columnName = identifier(column.get("column_name"));
      jdbcTemplate.execute(
        "ALTER TABLE " +
          tableName +
          " ALTER COLUMN " +
          columnName +
          " TYPE TEXT"
      );
    }

    if (!columns.isEmpty()) {
      log.info(
        "Converted {} bounded text columns to PostgreSQL TEXT",
        columns.size()
      );
    }
  }

  private String databaseProduct() {
    if (jdbcTemplate.getDataSource() == null) return "";
    try (var connection = jdbcTemplate.getDataSource().getConnection()) {
      return connection.getMetaData().getDatabaseProductName();
    } catch (Exception exception) {
      throw new IllegalStateException(
        "Unable to inspect the database",
        exception
      );
    }
  }

  private String identifier(Object value) {
    return '"' + String.valueOf(value).replace("\"", "\"\"") + '"';
  }
}
