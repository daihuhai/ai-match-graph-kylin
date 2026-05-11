package com.aimap.backend.controller;

import com.aimap.backend.common.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/graph")
public class GraphController {
  private String edgeId() {
    return "e-" + UUID.randomUUID().toString().substring(0, 8);
  }

  @GetMapping("/person/{id}")
  public ApiResponse<Map<String, Object>> personGraph(@PathVariable("id") String personId) {
    return ApiResponse.ok(Map.of(
        "nodes", List.of(
            Map.of("id", personId, "label", "个人", "type", "Person"),
            Map.of("id", "skill-vue", "label", "Vue 3", "type", "Skill"),
            Map.of("id", "skill-ts", "label", "TypeScript", "type", "Skill"),
            Map.of("id", "skill-spring", "label", "Spring Boot", "type", "Skill"),
            Map.of("id", "proj-1", "label", "图谱系统", "type", "Project")
        ),
        "edges", List.of(
            Map.of("id", edgeId(), "source", personId, "target", "skill-vue", "relation", "HAS_SKILL"),
            Map.of("id", edgeId(), "source", personId, "target", "skill-ts", "relation", "HAS_SKILL"),
            Map.of("id", edgeId(), "source", personId, "target", "skill-spring", "relation", "HAS_SKILL"),
            Map.of("id", edgeId(), "source", "proj-1", "target", "skill-vue", "relation", "APPLIED_IN")
        )));
  }

  @GetMapping("/job/{id}")
  public ApiResponse<Map<String, Object>> jobGraph(@PathVariable("id") String jobId) {
    return ApiResponse.ok(Map.of(
        "nodes", List.of(
            Map.of("id", jobId, "label", "岗位", "type", "Job"),
            Map.of("id", "skill-vue", "label", "Vue 3", "type", "Skill"),
            Map.of("id", "skill-ts", "label", "TypeScript", "type", "Skill"),
            Map.of("id", "skill-g6", "label", "AntV G6", "type", "Skill")
        ),
        "edges", List.of(
            Map.of("id", edgeId(), "source", jobId, "target", "skill-vue", "relation", "REQUIRES_SKILL"),
            Map.of("id", edgeId(), "source", jobId, "target", "skill-ts", "relation", "REQUIRES_SKILL"),
            Map.of("id", edgeId(), "source", jobId, "target", "skill-g6", "relation", "REQUIRES_SKILL")
        )));
  }

  public record ExpandReq(String subject, String nodeId) {}

  @PostMapping("/expand")
  public ApiResponse<Map<String, Object>> expand(@RequestBody ExpandReq req) {
    if (req.nodeId() != null && req.nodeId().startsWith("skill-")) {
      return ApiResponse.ok(Map.of(
          "nodes", List.of(
              Map.of("id", "skill-echarts", "label", "ECharts", "type", "Skill"),
              Map.of("id", "skill-pinia", "label", "Pinia", "type", "Skill")
          ),
          "edges", List.of(
              Map.of("id", edgeId(), "source", req.nodeId(), "target", "skill-echarts", "relation", "DEPENDS_ON"),
              Map.of("id", edgeId(), "source", req.nodeId(), "target", "skill-pinia", "relation", "DEPENDS_ON")
          )));
    }
    return ApiResponse.ok(Map.of(
        "nodes", List.of(Map.of("id", "cat-graph", "label", "图谱能力", "type", "Category")),
        "edges", List.of(Map.of("id", edgeId(), "source", "skill-g6", "target", "cat-graph", "relation", "BELONGS_TO"))));
  }
}
