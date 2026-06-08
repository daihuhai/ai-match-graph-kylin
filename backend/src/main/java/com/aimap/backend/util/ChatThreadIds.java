package com.aimap.backend.util;

import org.springframework.util.StringUtils;

public final class ChatThreadIds {

  private ChatThreadIds() {}

  public static String buildId(String personAccount, String companyAccount, String contextRecordId) {
    return "chat:"
        + personAccount.trim().toLowerCase()
        + "::"
        + companyAccount.trim().toLowerCase()
        + "::"
        + contextRecordId.trim().toLowerCase();
  }

  public static void requireNonBlank(String value, String field) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(field + " 不能为空");
    }
  }
}
