package com.aimap.backend.controller;

import com.aimap.backend.auth.BearerUserResolver;
import com.aimap.backend.auth.BearerUserResolver.LoggedUser;
import com.aimap.backend.common.ApiResponse;
import com.aimap.backend.service.InMemoryDataService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/match")
public class MatchController {
  private final InMemoryDataService dataService;

  /** minScore 0–100：仅返回霍兰德匹配度大于等于该阈值的记录。 */
  public record RecommendReq(Integer minScore) {}

  public MatchController(InMemoryDataService dataService) {
    this.dataService = dataService;
  }

  @PostMapping("/recommend-jobs")
  public ApiResponse<List<Map<String, Object>>> recommendJobs(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestBody(required = false) RecommendReq body) {
    Optional<LoggedUser> user = BearerUserResolver.fromAuthorization(authorization);
    int min = body != null && body.minScore() != null ? Math.max(0, Math.min(100, body.minScore())) : 0;
    return ApiResponse.ok(dataService.recommendJobs(user, min));
  }

  @PostMapping("/recommend-candidates")
  public ApiResponse<List<Map<String, Object>>> recommendCandidates(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestBody(required = false) RecommendReq body) {
    Optional<LoggedUser> user = BearerUserResolver.fromAuthorization(authorization);
    int min = body != null && body.minScore() != null ? Math.max(0, Math.min(100, body.minScore())) : 0;
    return ApiResponse.ok(dataService.recommendCandidates(user, min));
  }

  @GetMapping("/{id}/detail")
  public ApiResponse<Map<String, Object>> detail(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable("id") String recordId) {
    Optional<LoggedUser> user = BearerUserResolver.fromAuthorization(authorization);
    if (recordId.startsWith("rec-job-") || recordId.startsWith("rec-cand-")) {
      return ApiResponse.ok(dataService.legacyMatchDetail(recordId));
    }
    if (recordId.startsWith("job-")) {
      return ApiResponse.ok(dataService.matchDetailForJob(recordId, user));
    }
    if (recordId.startsWith("cand-")) {
      return ApiResponse.ok(dataService.matchDetailForCandidate(recordId, user));
    }
    throw new IllegalArgumentException("未知的匹配记录 ID");
  }
}
