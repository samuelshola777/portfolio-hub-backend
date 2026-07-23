package com.portfolio_hub.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class DatabaseConfig {

  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  DataSource dataSource(
    DataSourceProperties properties,
    Environment environment
  ) {
    ResolvedDatabaseUrl resolvedUrl = resolveDatabaseUrl(environment);
    properties.setUrl(resolvedUrl.jdbcUrl());

    String username = firstNonBlank(
      environment.getProperty("SPRING_DATASOURCE_USERNAME"),
      environment.getProperty("DB_USERNAME"),
      resolvedUrl.username()
    );
    String password = firstNonBlank(
      environment.getProperty("SPRING_DATASOURCE_PASSWORD"),
      environment.getProperty("DB_PASSWORD"),
      resolvedUrl.password()
    );

    if (username != null) {
      properties.setUsername(username);
    }
    if (password != null) {
      properties.setPassword(password);
    }

    return properties
      .initializeDataSourceBuilder()
      .type(HikariDataSource.class)
      .build();
  }

  private ResolvedDatabaseUrl resolveDatabaseUrl(Environment environment) {
    String configuredUrl = firstNonBlank(
      environment.getProperty("SPRING_DATASOURCE_URL")
    );
    if (configuredUrl != null) {
      return new ResolvedDatabaseUrl(configuredUrl, null, null);
    }

    String connectionString = firstNonBlank(
      environment.getProperty("DATABASE_URL")
    );
    if (connectionString != null) {
      return parseConnectionString(connectionString);
    }

    String host = firstNonBlank(
      environment.getProperty("DB_HOST"),
      "localhost"
    );
    String port = firstNonBlank(environment.getProperty("DB_PORT"), "5432");
    String database = firstNonBlank(
      environment.getProperty("DB_NAME"),
      "portfolio_hub"
    );
    return new ResolvedDatabaseUrl(
      "jdbc:postgresql://" + host + ":" + port + "/" + database,
      null,
      null
    );
  }

  private ResolvedDatabaseUrl parseConnectionString(String connectionString) {
    if (connectionString.startsWith("jdbc:")) {
      return new ResolvedDatabaseUrl(connectionString, null, null);
    }

    try {
      URI uri = URI.create(connectionString);
      String scheme = uri.getScheme();
      if (
        !"postgres".equalsIgnoreCase(scheme) &&
        !"postgresql".equalsIgnoreCase(scheme)
      ) {
        throw new IllegalArgumentException("unsupported scheme");
      }

      String path = uri.getRawPath();
      if (path == null || path.isBlank() || "/".equals(path)) {
        throw new IllegalArgumentException("database name is missing");
      }

      String authority = uri.getRawAuthority();
      if (authority == null || authority.isBlank()) {
        throw new IllegalArgumentException("database host is missing");
      }

      String userInfo = uri.getUserInfo();
      String hostAndPort = authority;
      String username = null;
      String password = null;
      if (userInfo != null) {
        int separator = userInfo.indexOf(':');
        if (separator >= 0) {
          username = userInfo.substring(0, separator);
          password = userInfo.substring(separator + 1);
        } else {
          username = userInfo;
        }
        hostAndPort = authority.substring(authority.lastIndexOf('@') + 1);
      }

      String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
      return new ResolvedDatabaseUrl(
        "jdbc:postgresql://" + hostAndPort + path + query,
        username,
        password
      );
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException(
        "DATABASE_URL must be a valid PostgreSQL connection string",
        exception
      );
    }
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private record ResolvedDatabaseUrl(
    String jdbcUrl,
    String username,
    String password
  ) {}
}
