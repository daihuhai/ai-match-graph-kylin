package com.aimap.backend.controller;

import com.aimap.backend.common.ApiResponse;
import com.aimap.backend.service.ArkLlmService;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/llm")
public class LlmController {
  private final ArkLlmService arkLlmService;

  public LlmController(ArkLlmService arkLlmService) {
    this.arkLlmService = arkLlmService;
  }

  public record PromptReq(@NotBlank String text) {}

  /** 不调用远端模型；用于部署后确认密钥是否已被进程加载（不返回任何密钥字段）。 */
  @GetMapping("/status")
  public ApiResponse<Map<String, Object>> status() {
    return ApiResponse.ok(
        Map.of("arkEnabled", arkLlmService.enabled(), "model", arkLlmService.getModel()));
  }

  @PostMapping("/extract")
  public ApiResponse<Map<String, Object>> extract(@RequestBody @Validated PromptReq req) {
    String resp = arkLlmService.chat("请抽取技能、经历、学历等结构化字段并输出中文摘要：" + req.text());
    return ApiResponse.ok(Map.of("text", resp, "provider", "volcengine-ark", "enabled", arkLlmService.enabled()));
  }

  @PostMapping("/explain-match")
  public ApiResponse<Map<String, Object>> explain(@RequestBody @Validated PromptReq req) {
    String resp = arkLlmService.chat("请解释候选人与岗位匹配分数依据，强调可解释性：" + req.text());
    return ApiResponse.ok(Map.of("text", resp, "provider", "volcengine-ark", "enabled", arkLlmService.enabled()));
  }

  @PostMapping("/generate-suggestion")
  public ApiResponse<Map<String, Object>> suggestion(@RequestBody @Validated PromptReq req) {
    String resp = arkLlmService.chat("请生成候选人能力提升建议：" + req.text());
    return ApiResponse.ok(Map.of("text", resp, "provider", "volcengine-ark", "enabled", arkLlmService.enabled()));
  }
}
