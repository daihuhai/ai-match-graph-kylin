package com.aimap.backend.controller;

import com.aimap.backend.common.ApiResponse;
import com.aimap.backend.service.InMemoryDataService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final InMemoryDataService dataService;

  public AuthController(InMemoryDataService dataService) {
    this.dataService = dataService;
  }

  public record AuthReq(@NotBlank String account, @NotBlank String password, @NotBlank String userType) {}

  @PostMapping("/register")
  public ApiResponse<Map<String, Object>> register(@RequestBody @Validated AuthReq req) {
    dataService.register(req.account(), req.password(), req.userType());
    return ApiResponse.ok(Map.of("account", req.account(), "userType", req.userType()));
  }

  @PostMapping("/login")
  public ApiResponse<Map<String, Object>> login(@RequestBody @Validated AuthReq req) {
    InMemoryDataService.User user = dataService.login(req.account(), req.password(), req.userType());
    List<String> permissions = "ADMIN".equals(user.userType())
        ? List.of("ADMIN_USERS_VIEW", "ADMIN_DOCS_VIEW", "ADMIN_DATA_VIEW", "ADMIN_MATCH_VIEW", "ADMIN_MONITOR_VIEW", "ADMIN_AUDIT_VIEW")
        : List.of();
    return ApiResponse.ok(Map.of(
        "token", "token-" + user.userType().toLowerCase() + "-" + user.account(),
        "userType", user.userType(),
        "userId", user.account(),
        "permissions", permissions));
  }

  @PostMapping("/logout")
  public ApiResponse<Map<String, Object>> logout() {
    return ApiResponse.ok(Map.of("success", true));
  }
}
