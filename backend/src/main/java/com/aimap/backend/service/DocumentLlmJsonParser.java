package com.aimap.backend.service;

import com.aimap.backend.util.JsonObjectExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DocumentLlmJsonParser {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public record ParsedDoc(String critiqueMarkdown, Map<String, Integer> riasec, List<String> skills) {
    public ParsedDoc {
      skills = skills == null ? List.of() : List.copyOf(skills);
    }
  }

  public ParsedDoc parse(String rawLlmText, boolean resume) {
    Optional<String> json = JsonObjectExtractor.firstBalancedObject(rawLlmText == null ? "" : rawLlmText);
    if (json.isEmpty()) {
      return new ParsedDoc(rawLlmText == null ? "" : rawLlmText.trim(), defaultRiasec(), List.of());
    }
    try {
      JsonNode root = MAPPER.readTree(json.get());
      Map<String, Integer> riasec = readRiasec(root.path("riasec"));
      if (riasec.isEmpty()) {
        riasec = defaultRiasec();
      }
      String critique = resume ? formatResumeCritique(root.path("critique")) : formatJobCritique(root.path("critique"));
      if (critique.isBlank()) {
        critique = rawLlmText.trim();
      }
      List<String> skills = resume ? readTextSkills(root.path("skills")) : readTextSkills(root.path("critique").path("requiredSkills"));
      return new ParsedDoc(critique, riasec, skills);
    } catch (Exception e) {
      return new ParsedDoc(rawLlmText == null ? "" : rawLlmText.trim(), defaultRiasec(), List.of());
    }
  }

  private static List<String> readTextSkills(JsonNode node) {
    List<String> out = new ArrayList<>();
    if (node == null || !node.isArray() || node.isEmpty()) {
      return out;
    }
    for (JsonNode n : node) {
      if (n.isTextual()) {
        String t = n.asText().trim();
        if (!t.isEmpty() && out.size() < 24) {
          out.add(t);
        }
      }
    }
    return out;
  }

  private static Map<String, Integer> defaultRiasec() {
    return Map.of("r", 40, "i", 45, "a", 35, "s", 45, "e", 38, "c", 42);
  }

  private static Map<String, Integer> readRiasec(JsonNode node) {
    Map<String, Integer> m = new LinkedHashMap<>();
    if (node == null || !node.isObject()) {
      return m;
    }
    putDim(m, "r", node, "R", "r");
    putDim(m, "i", node, "I", "i");
    putDim(m, "a", node, "A", "a");
    putDim(m, "s", node, "S", "s");
    putDim(m, "e", node, "E", "e");
    putDim(m, "c", node, "C", "c");
    return m;
  }

  private static void putDim(Map<String, Integer> m, String key, JsonNode node, String upper, String lower) {
    int v = 0;
    if (node.has(upper)) {
      v = node.get(upper).asInt(0);
    } else if (node.has(lower)) {
      v = node.get(lower).asInt(0);
    }
    m.put(key, Math.max(0, Math.min(100, v)));
  }

  private static String formatResumeCritique(JsonNode critique) {
    if (critique == null || !critique.isObject()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    appendBullets(sb, "优势", critique.path("strengths"));
    appendBullets(sb, "不足", critique.path("weaknesses"));
    appendBullets(sb, "改进建议", critique.path("improvements"));
    return sb.toString().trim();
  }

  private static String formatJobCritique(JsonNode critique) {
    if (critique == null || !critique.isObject()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    if (critique.has("summary") && critique.get("summary").isTextual()) {
      sb.append("摘要：").append(critique.get("summary").asText()).append("\n\n");
    }
    appendBullets(sb, "核心能力要求", critique.path("requiredSkills"));
    appendBullets(sb, "岗位画像补充", critique.path("notes"));
    return sb.toString().trim();
  }

  private static void appendBullets(StringBuilder sb, String title, JsonNode arr) {
    if (arr == null || !arr.isArray() || arr.isEmpty()) {
      return;
    }
    sb.append("【").append(title).append("】\n");
    for (JsonNode n : arr) {
      if (n.isTextual()) {
        sb.append("- ").append(n.asText()).append('\n');
      }
    }
    sb.append('\n');
  }
}
