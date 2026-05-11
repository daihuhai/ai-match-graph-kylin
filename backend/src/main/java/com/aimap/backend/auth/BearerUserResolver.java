package com.aimap.backend.auth;

import java.util.Optional;
import org.springframework.util.StringUtils;

public final class BearerUserResolver {

  private BearerUserResolver() {}

  public record LoggedUser(String userType, String account) {
    public String userKey() {
      return userType + ":" + account;
    }
  }

  /**
   * Parses {@code Authorization: Bearer token-person-demo} style tokens issued by {@link
   * com.aimap.backend.controller.AuthController}.
   */
  public static Optional<LoggedUser> fromAuthorization(String authorization) {
    if (!StringUtils.hasText(authorization) || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return Optional.empty();
    }
    String token = authorization.substring(7).trim();
    if (!token.startsWith("token-")) {
      return Optional.empty();
    }
    String tail = token.substring("token-".length());
    int dash = tail.indexOf('-');
    if (dash <= 0 || dash >= tail.length() - 1) {
      return Optional.empty();
    }
    String typeLower = tail.substring(0, dash);
    String account = tail.substring(dash + 1);
    if (!StringUtils.hasText(account)) {
      return Optional.empty();
    }
    return Optional.of(new LoggedUser(typeLower.toUpperCase(), account));
  }
}
