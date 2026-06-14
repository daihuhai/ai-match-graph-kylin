package com.aimap.backend.controller;

import com.aimap.backend.auth.BearerUserResolver;
import com.aimap.backend.common.ApiResponse;
import com.aimap.backend.service.AdminUserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
  public record SaveUserReq(
      Long id,
      @NotBlank String account,
      @NotBlank String userType,
      @NotBlank String status,
      String phone,
      String email) {}

  public record StatusReq(@NotBlank String status) {}

  private final AdminUserService adminUserService;

  public AdminUserController(AdminUserService adminUserService) {
    this.adminUserService = adminUserService;
  }

  @GetMapping
  public ApiResponse<List<AdminUserService.AdminUserRow>> list(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    BearerUserResolver.requireAdmin(authorization);
    return ApiResponse.ok(adminUserService.list());
  }

  @PostMapping
  public ApiResponse<AdminUserService.AdminUserRow> create(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @RequestBody @Validated SaveUserReq req) {
    BearerUserResolver.requireAdmin(authorization);
    return ApiResponse.ok(
        adminUserService.save(
            new AdminUserService.SaveUserReq(null, req.account(), req.userType(), req.status(), req.phone(), req.email())));
  }

  @PutMapping("/{id}")
  public ApiResponse<AdminUserService.AdminUserRow> update(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @PathVariable @NotNull Long id, @RequestBody @Validated SaveUserReq req) {
    BearerUserResolver.requireAdmin(authorization);
    return ApiResponse.ok(
        adminUserService.save(
            new AdminUserService.SaveUserReq(id, req.account(), req.userType(), req.status(), req.phone(), req.email())));
  }

  @PutMapping("/{id}/status")
  public ApiResponse<AdminUserService.AdminUserRow> setStatus(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @PathVariable @NotNull Long id, @RequestBody @Validated StatusReq req) {
    BearerUserResolver.requireAdmin(authorization);
    return ApiResponse.ok(adminUserService.setStatus(id, req.status()));
  }

  @PostMapping("/{id}/reset-password")
  public ApiResponse<Map<String, Object>> resetPassword(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @PathVariable @NotNull Long id) {
    BearerUserResolver.requireAdmin(authorization);
    adminUserService.resetPassword(id);
    return ApiResponse.ok(Map.of("success", true, "message", "密码已重置为系统默认密码"));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Map<String, Object>> delete(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @PathVariable @NotNull Long id) {
    BearerUserResolver.requireAdmin(authorization);
    adminUserService.delete(id);
    return ApiResponse.ok(Map.of("success", true));
  }
}
