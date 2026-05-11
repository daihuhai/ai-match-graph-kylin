package com.aimap.backend.util;

import java.util.Optional;

public final class JsonObjectExtractor {

  private JsonObjectExtractor() {}

  /** Returns the first top-level balanced JSON object substring, if any. */
  public static Optional<String> firstBalancedObject(String text) {
    if (text == null) {
      return Optional.empty();
    }
    int start = text.indexOf('{');
    if (start < 0) {
      return Optional.empty();
    }
    int depth = 0;
    boolean inString = false;
    boolean escape = false;
    for (int j = start; j < text.length(); j++) {
      char c = text.charAt(j);
      if (escape) {
        escape = false;
        continue;
      }
      if (c == '\\' && inString) {
        escape = true;
        continue;
      }
      if (c == '"') {
        inString = !inString;
        continue;
      }
      if (inString) {
        continue;
      }
      if (c == '{') {
        depth++;
      } else if (c == '}') {
        depth--;
        if (depth == 0) {
          return Optional.of(text.substring(start, j + 1));
        }
      }
    }
    return Optional.empty();
  }
}
