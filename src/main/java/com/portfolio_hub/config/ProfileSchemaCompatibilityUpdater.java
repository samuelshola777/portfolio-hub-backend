package com.portfolio_hub.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class ProfileSchemaCompatibilityUpdater implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    updateProfileEntriesTable();
    updateSkillsTable();

    log.info("Profile database compatibility update completed");
  }

  private void updateProfileEntriesTable() {
    // Add new optional columns
    jdbcTemplate.execute(
      """
      ALTER TABLE profile_entries
      ADD COLUMN IF NOT EXISTS thumbnail_url VARCHAR(2048)
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE profile_entries
      ADD COLUMN IF NOT EXISTS supporting_document_url VARCHAR(2048)
      """
    );

    // Add published as nullable first
    jdbcTemplate.execute(
      """
      ALTER TABLE profile_entries
      ADD COLUMN IF NOT EXISTS published BOOLEAN
      """
    );

    // Give existing records a value
    jdbcTemplate.update(
      """
      UPDATE profile_entries
      SET published = TRUE
      WHERE published IS NULL
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE profile_entries
      ALTER COLUMN published SET DEFAULT TRUE
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE profile_entries
      ALTER COLUMN published SET NOT NULL
      """
    );

    // Safely add sort order
    jdbcTemplate.execute(
      """
      ALTER TABLE profile_entries
      ADD COLUMN IF NOT EXISTS sort_order INTEGER
      """
    );

    jdbcTemplate.update(
      """
      UPDATE profile_entries
      SET sort_order = 0
      WHERE sort_order IS NULL
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE profile_entries
      ALTER COLUMN sort_order SET DEFAULT 0
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE profile_entries
      ALTER COLUMN sort_order SET NOT NULL
      """
    );
  }

  private void updateSkillsTable() {
    jdbcTemplate.execute(
      """
      ALTER TABLE skills
      ADD COLUMN IF NOT EXISTS icon_url VARCHAR(2048)
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE skills
      ADD COLUMN IF NOT EXISTS featured BOOLEAN
      """
    );

    jdbcTemplate.update(
      """
      UPDATE skills
      SET featured = FALSE
      WHERE featured IS NULL
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE skills
      ALTER COLUMN featured SET DEFAULT FALSE
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE skills
      ALTER COLUMN featured SET NOT NULL
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE skills
      ADD COLUMN IF NOT EXISTS sort_order INTEGER
      """
    );

    jdbcTemplate.update(
      """
      UPDATE skills
      SET sort_order = 0
      WHERE sort_order IS NULL
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE skills
      ALTER COLUMN sort_order SET DEFAULT 0
      """
    );

    jdbcTemplate.execute(
      """
      ALTER TABLE skills
      ALTER COLUMN sort_order SET NOT NULL
      """
    );
  }
}
