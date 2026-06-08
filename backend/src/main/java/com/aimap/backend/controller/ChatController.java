package com.aimap.backend.controller;

import com.aimap.backend.auth.BearerUserResolver;
import com.aimap.backend.auth.BearerUserResolver.LoggedUser;
import com.aimap.backend.common.ApiResponse;
import com.aimap.backend.service.ChatService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

  private final ChatService chatService;

  public record EnsureThreadReq(
      @NotBlank String personAccount,
      @NotBlank String companyAccount,
      @NotBlank String contextRecordId,
      String personName,
      String companyName,
      String contextTitle,
      String contextOrg) {}

  public record SendMessageReq(@NotBlank String text) {}

  public record MarkReadReq(Long lastMessageId) {}

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @GetMapping("/threads")
  public ApiResponse<List<Map<String, Object>>> listThreads(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    LoggedUser user = chatService.requireChatUser(BearerUserResolver.fromAuthorization(authorization));
    return ApiResponse.ok(chatService.listThreads(user));
  }

  @PostMapping("/threads")
  public ApiResponse<Map<String, Object>> ensureThread(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestBody @Validated EnsureThreadReq req) {
    LoggedUser user = chatService.requireChatUser(BearerUserResolver.fromAuthorization(authorization));
    return ApiResponse.ok(
        chatService.ensureThread(
            user,
            req.personAccount(),
            req.personName(),
            req.companyAccount(),
            req.companyName(),
            req.contextRecordId(),
            req.contextTitle(),
            req.contextOrg()));
  }

  @GetMapping("/threads/{threadId}/messages")
  public ApiResponse<List<Map<String, Object>>> listMessages(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable String threadId) {
    LoggedUser user = chatService.requireChatUser(BearerUserResolver.fromAuthorization(authorization));
    return ApiResponse.ok(chatService.listMessages(user, threadId));
  }

  @PostMapping("/threads/{threadId}/messages")
  public ApiResponse<Map<String, Object>> sendMessage(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable String threadId,
      @RequestBody @Validated SendMessageReq req) {
    LoggedUser user = chatService.requireChatUser(BearerUserResolver.fromAuthorization(authorization));
    return ApiResponse.ok(chatService.sendMessage(user, threadId, req.text()));
  }

  @PostMapping("/threads/{threadId}/read")
  public ApiResponse<Map<String, Object>> markRead(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable String threadId,
      @RequestBody(required = false) MarkReadReq req) {
    LoggedUser user = chatService.requireChatUser(BearerUserResolver.fromAuthorization(authorization));
    Long lastId = req != null ? req.lastMessageId() : null;
    chatService.markRead(user, threadId, lastId);
    return ApiResponse.ok(Map.of("success", true));
  }
}
