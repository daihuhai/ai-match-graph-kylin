package com.aimap.backend.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.util.StringUtils;

/**
 * 登录 Token 中用户名的 ASCII 安全编码（Base64URL）。新格式为 {@code token-person.<payload>}，与旧格式
 * {@code token-person-demo}（中间为连字符且无 Base64 段）区分，避免纯英文账号子串被误解码。
 */
public final class TokenAccountCodec {

  private TokenAccountCodec() {}

  public static String encode(String account) {
    if (!StringUtils.hasText(account)) {
      return "";
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(account.getBytes(StandardCharsets.UTF_8));
  }
}
