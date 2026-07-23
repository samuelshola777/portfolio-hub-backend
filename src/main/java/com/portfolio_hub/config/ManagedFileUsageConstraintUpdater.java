package com.portfolio_hub.config;

import com.portfolio_hub.utils.fileupload.FileUsageType;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManagedFileUsageConstraintUpdater implements ApplicationRunner {

  private static final String CONSTRAINT_NAME =
    "managed_files_usage_type_check";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    try {
      updateManagedFileUsageConstraint();
    } catch (Exception exception) {
      log.error(
        "Unable to update managed_files usage type constraint",
        exception
      );

      throw new IllegalStateException(
        "Could not update managed_files_usage_type_check",
        exception
      );
    }
  }

  private void updateManagedFileUsageConstraint() {
    String allowedValues = Arrays.stream(FileUsageType.values())
      .map(Enum::name)
      .map(value -> "'" + value + "'")
      .collect(Collectors.joining(", "));

    String currentDefinition = findCurrentConstraintDefinition();

    boolean constraintIsCurrent =
      currentDefinition != null &&
      Arrays.stream(FileUsageType.values())
        .map(Enum::name)
        .allMatch(currentDefinition::contains);

    if (constraintIsCurrent) {
      log.info("{} already contains all file usage types", CONSTRAINT_NAME);
      return;
    }

    log.info("Updating {}", CONSTRAINT_NAME);

    jdbcTemplate.execute(
      """
      ALTER TABLE managed_files
      DROP CONSTRAINT IF EXISTS managed_files_usage_type_check
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE managed_files
      ADD CONSTRAINT managed_files_usage_type_check
      CHECK (usage_type IN (%s))
      """.formatted(allowedValues)
    );

    log.info("{} updated successfully", CONSTRAINT_NAME);
  }

  private String findCurrentConstraintDefinition() {
    return jdbcTemplate.query(
      """
      SELECT pg_get_constraintdef(constraint_data.oid)
      FROM pg_constraint constraint_data
      WHERE constraint_data.conname = ?
        AND constraint_data.conrelid =
            'managed_files'::regclass
      """,
      preparedStatement -> preparedStatement.setString(1, CONSTRAINT_NAME),
      resultSet -> resultSet.next() ? resultSet.getString(1) : null
    );
  }
}
