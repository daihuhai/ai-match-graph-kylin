package com.aimap.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway uses PostgreSQL-compatible JDBC. For KingbaseES / openGauss style databases, keep
 * Flyway on the PostgreSQL driver even if the application datasource URL later changes to a
 * vendor-specific JDBC prefix.
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
    ds.setJdbcUrl(toFlywayJdbcUrl(url));
    ds.setUsername(username);
    ds.setPassword(password);
    ds.setDriverClassName("org.postgresql.Driver");
    return ds;
  }

  static String toFlywayJdbcUrl(String jdbcUrl) {
    if (jdbcUrl == null) {
      return null;
    }
    return jdbcUrl.replaceFirst("^jdbc:(opengauss|kingbase8):", "jdbc:postgresql:");
  }
}
