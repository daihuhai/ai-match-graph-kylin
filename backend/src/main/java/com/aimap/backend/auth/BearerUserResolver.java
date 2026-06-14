package com.aimap.backend.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

public final class BearerUserResolver {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String TOKEN_PREFIX = "aimap";
  private static final String SECRET =
      System.getenv().getOrDefault("AIMAP_TOKEN_SECRET", "aimap-dev-token-secret-change-me");
  private static final long TTL_SECONDS = parseTtlSeconds();

  private BearerUserResolver() {}

  public record LoggedUser(String userType, String account) {
    public String userKey() {
      return userType + ":" + account;
    }
  }

  public static String issueToken(String userType, String account) {
    if (!StringUtils.hasText(userType) || !StringUtils.hasText(account)) {
      throw new IllegalArgumentException("用户信息不完整，无法签发令牌");
    }
    long issuedAt = Instant.now().getEpochSecond();
    long expireAt = issuedAt + TTL_SECONDS;
    try {
      String payload =
          OBJECT_MAPPER.writeValueAsString(
              Map.of("userType", userType.trim().toUpperCase(), "account", account.trim(), "iat", issuedAt, "exp", expireAt));
      String payloadBase64 =
          Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
      String signature = sign(payloadBase64);
      return TOKEN_PREFIX + "." + payloadBase64 + "." + signature;
    } catch (Exception e) {
      throw new IllegalStateException("签发令牌失败", e);
    }
  }

  public static Optional<LoggedUser> fromAuthorization(String authorization) {
    if (!StringUtils.hasText(authorization) || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return Optional.empty();
    }
    String token = authorization.substring(7).trim();
    if (token.startsWith(TOKEN_PREFIX + ".")) {
      return parseSignedToken(token);
    }
    if (token.startsWith("token-")) {
      return parseLegacyToken(token);
    }
    return Optional.empty();
  }

  public static LoggedUser requireUser(String authorization) {
    return fromAuthorization(authorization).orElseThrow(() -> new IllegalArgumentException("请先登录"));
  }

  public static LoggedUser requireAdmin(String authorization) {
    LoggedUser user = requireUser(authorization);
    if (!"ADMIN".equals(user.userType())) {
      throw new IllegalArgumentException("当前账号无权访问管理端接口");
    }
    return user;
  }

  private static Optional<LoggedUser> parseSignedToken(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3 || !TOKEN_PREFIX.equals(parts[0])) {
      return Optional.empty();
    }
    String payloadBase64 = parts[1];
    String signature = parts[2];
    if (!constantTimeEquals(signature, sign(payloadBase64))) {
      return Optional.empty();
    }
    try {
      String payloadJson =
          new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);
      Map<String, Object> payload =
          OBJECT_MAPPER.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
      String userType = textValue(payload.get("userType"));
      String account = textValue(payload.get("account"));
      long expireAt = longValue(payload.get("exp"));
      if (!StringUtils.hasText(userType) || !StringUtils.hasText(account)) {
        return Optional.empty();
      }
      if (expireAt <= Instant.now().getEpochSecond()) {
        return Optional.empty();
      }
      return Optional.of(new LoggedUser(userType.toUpperCase(), account));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private static Optional<LoggedUser> parseLegacyToken(String token) {
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

  private static String sign(String payloadBase64) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] digest = mac.doFinal(payloadBase64.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    } catch (Exception e) {
      throw new IllegalStateException("令牌签名失败", e);
    }
  }

  private static boolean constantTimeEquals(String a, String b) {
    return MessageDigest.isEqual(
        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }

  private static String textValue(Object value) {
    return value == null ? "" : String.valueOf(value).trim();
  }

  private static long longValue(Object value) {
    if (value instanceof Number n) {
      return n.longValue();
    }
    if (value == null) {
      return 0L;
    }
    try {
      return Long.parseLong(String.valueOf(value));
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private static long parseTtlSeconds() {
    String raw = System.getenv("AIMAP_TOKEN_TTL_SECONDS");
    if (!StringUtils.hasText(raw)) {
      return 12 * 60 * 60L;
    }
    try {
      long ttl = Long.parseLong(raw.trim());
      return ttl > 0 ? ttl : 12 * 60 * 60L;
    } catch (NumberFormatException e) {
      return 12 * 60 * 60L;
    }
  }
}
