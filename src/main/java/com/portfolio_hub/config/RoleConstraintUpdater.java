package com.portfolio_hub.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate ddl-auto=update does not revise an existing PostgreSQL enum-style
 * CHECK constraint when a Java enum gains a value. This small, idempotent
 * startup repair keeps the existing database compatible without Flyway.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleConstraintUpdater implements ApplicationRunner {

  private static final String REQUIRED_DEFINITION =
    "CHECK (role IN ('USER', 'PROFESSIONAL', 'BUSINESS_OWNER', 'SUPER_ADMIN'))";

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    String product = jdbcTemplate.getDataSource() == null
      ? ""
      : databaseProduct();
    if (!product.toLowerCase().contains("postgresql")) return;

    List<String> definitions = jdbcTemplate.queryForList(
      """
      select pg_get_constraintdef(c.oid)
      from pg_constraint c
      where c.conrelid = 'users'::regclass
        and c.conname = 'users_role_check'
      """,
      String.class
    );

    boolean current = definitions
      .stream()
      .anyMatch(
        value ->
          value.contains("USER") &&
          value.contains("PROFESSIONAL") &&
          value.contains("BUSINESS_OWNER") &&
          value.contains("SUPER_ADMIN")
      );
    if (current) return;

    log.info(
      "Updating PostgreSQL users_role_check for the current account roles"
    );
    jdbcTemplate.execute(
      "alter table users drop constraint if exists users_role_check"
    );
    jdbcTemplate.execute(
      "alter table users add constraint users_role_check " + REQUIRED_DEFINITION
    );
  }

  private String databaseProduct() {
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
