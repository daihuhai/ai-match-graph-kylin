package com.aimap.backend.util;

import java.util.Map;

public final class RiasecMath {

  private RiasecMath() {}

  private static int v(Map<String, Integer> m, String k) {
    if (m == null) {
      return 0;
    }
    Integer x = m.get(k);
    if (x != null) {
      return clamp(x);
    }
    x = m.get(k.toUpperCase());
    return x == null ? 0 : clamp(x);
  }

  private static int clamp(int x) {
    return Math.max(0, Math.min(100, x));
  }

  /** 0–100: higher means closer profiles (same shape as existing match detail heuristic). */
  public static int similarity100(Map<String, Integer> a, Map<String, Integer> b) {
    if (a == null || a.isEmpty() || b == null || b.isEmpty()) {
      return 0;
    }
    String[] keys = {"r", "i", "a", "s", "e", "c"};
    int dist = 0;
    for (String k : keys) {
      dist += Math.abs(v(a, k) - v(b, k));
    }
    return Math.max(0, Math.min(100, Math.round(100 - dist / 6f)));
  }
}
