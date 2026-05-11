package com.aimap.backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class JsonHelper {

  private static final ObjectMapper OM = new ObjectMapper();

  private JsonHelper() {}

  public static String riasecToJson(Map<String, Integer> map) {
    if (map == null || map.isEmpty()) {
      return null;
    }
    try {
      return OM.writeValueAsString(map);
    } catch (Exception e) {
      return null;
    }
  }

  public static Map<String, Integer> parseRiasec(String json) {
    Map<String, Integer> out = new LinkedHashMap<>();
    if (!StringUtils.hasText(json)) {
      return out;
    }
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> raw = OM.readValue(json, Map.class);
      for (Map.Entry<String, Object> e : raw.entrySet()) {
        String k = e.getKey().toLowerCase();
        if (k.length() != 1 || "riasec".indexOf(k.charAt(0)) < 0) {
          continue;
        }
        int v = 0;
        Object o = e.getValue();
        if (o instanceof Number n) {
          v = n.intValue();
        } else if (o != null) {
          v = Integer.parseInt(o.toString());
        }
        out.put(k, Math.max(0, Math.min(100, v)));
      }
      return out;
    } catch (Exception e) {
      return out;
    }
  }

  public static String skillsToJson(List<String> skills) {
    if (skills == null || skills.isEmpty()) {
      return "[]";
    }
    try {
      return OM.writeValueAsString(skills);
    } catch (Exception e) {
      return "[]";
    }
  }

  public static List<String> parseSkills(String json) {
    if (!StringUtils.hasText(json)) {
      return List.of();
    }
    try {
      return OM.readValue(json, new TypeReference<ArrayList<String>>() {});
    } catch (Exception e) {
      return List.of();
    }
  }
}
