package com.aimap.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
   * 新格式：{@code Authorization: Bearer token-person.<Base64URL(UTF-8 用户名)>}（类型与 payload 之间为点号）。<br>
   * 旧格式：{@code Bearer token-person-demo}（类型与账号之间为连字符、账号段非 Base64），兼容种子与历史会话。
   */
  public static Optional<LoggedUser> fromAuthorization(String authorization) {
    if (!StringUtils.hasText(authorization) || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return Optional.empty();
    }
    String token = authorization.substring(7).trim();
    if (!token.startsWith("token-")) {
      return Optional.empty();
    }
    String rest = token.substring("token-".length());
    int dot = rest.indexOf('.');
    if (dot > 0 && dot < rest.length() - 1) {
      return parseDotForm(rest, dot);
    }
    return parseDashLegacyForm(rest);
  }

  private static Optional<LoggedUser> parseDotForm(String rest, int dot) {
    String typeLower = rest.substring(0, dot);
    String b64 = rest.substring(dot + 1);
    if (!StringUtils.hasText(typeLower) || !StringUtils.hasText(b64)) {
      return Optional.empty();
    }
    try {
      String account = new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
      if (!StringUtils.hasText(account)) {
        return Optional.empty();
      }
      return Optional.of(new LoggedUser(typeLower.toUpperCase(), account));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private static Optional<LoggedUser> parseDashLegacyForm(String rest) {
    int dash = rest.indexOf('-');
    if (dash <= 0 || dash >= rest.length() - 1) {
      return Optional.empty();
    }
    String typeLower = rest.substring(0, dash);
    String account = rest.substring(dash + 1);
    if (!StringUtils.hasText(account)) {
      return Optional.empty();
    }
    return Optional.of(new LoggedUser(typeLower.toUpperCase(), account));
  }
}
