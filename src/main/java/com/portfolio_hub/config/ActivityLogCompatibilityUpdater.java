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
@Order(110)
@RequiredArgsConstructor
public class ActivityLogCompatibilityUpdater implements ApplicationRunner {

  private static final String MIGRATION = "activity-log-materialization-v1";
  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!databaseProduct().toLowerCase().contains("postgresql")) return;
    jdbcTemplate.execute(
      """
      CREATE TABLE IF NOT EXISTS app_data_migrations (
          migration_key TEXT PRIMARY KEY,
          applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
      )
      """
    );
    Integer applied = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM app_data_migrations WHERE migration_key = ?",
      Integer.class,
      MIGRATION
    );
    if (applied != null && applied > 0) return;

    jdbcTemplate.update(
      """
      INSERT INTO audit_logs (id, created_at, updated_at, actor_id, target_user_id, action, description)
      SELECT 'registration-' || id, created_at, created_at, id, id,
             'USER_REGISTERED', full_name || ' created an account'
      FROM users WHERE deleted = FALSE
      ON CONFLICT (id) DO NOTHING
      """
    );
    jdbcTemplate.update(
      """
      INSERT INTO audit_logs (id, created_at, updated_at, actor_id, target_user_id, action, description)
      SELECT 'login-' || id, last_login_at, last_login_at, id, id,
             'USER_LOGGED_IN', full_name || ' signed in'
      FROM users WHERE deleted = FALSE AND last_login_at IS NOT NULL
      ON CONFLICT (id) DO NOTHING
      """
    );
    jdbcTemplate.update(
      "INSERT INTO app_data_migrations (migration_key) VALUES (?)",
      MIGRATION
    );
    log.info(
      "Existing registration and login activity was materialized for hard CRUD management"
    );
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
}
