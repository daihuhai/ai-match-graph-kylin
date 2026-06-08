package com.aimap.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway uses PostgreSQL-compatible JDBC. With openGauss, ensure the app user's
 * {@code search_path} is {@code public} only (not {@code "$user",public}) so Flyway does not
 * issue {@code SET ROLE}; see ensure-dev-db.ps1.
 */
@Configuration
public class FlywayDataSourceConfig {

  @Bean
  @FlywayDataSource
  public DataSource flywayDataSource(
      @Value("${spring.datasource.url}") String url,
      @Value("${spring.datasource.username}") String username,
      @Value("${spring.datasource.password}") String password) {
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(toPostgresqlJdbcUrl(url));
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName("org.postgresql.Driver");
    return ds;
  }

  static String toPostgresqlJdbcUrl(String openGaussUrl) {
    if (openGaussUrl == null) {
      return null;
    }
    return openGaussUrl.replaceFirst("^jdbc:opengauss:", "jdbc:postgresql:");
  }
}
