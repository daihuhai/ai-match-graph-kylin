package com.aimap.backend.service;

import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.content.InputContentItemText;
import com.volcengine.ark.runtime.model.responses.content.OutputContentItem;
import com.volcengine.ark.runtime.model.responses.content.OutputContentItemText;
import com.volcengine.ark.runtime.model.responses.item.BaseItem;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.model.responses.item.ItemOutputMessage;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArkLlmService {
  private static final Logger log = LoggerFactory.getLogger(ArkLlmService.class);

  private final ArkService arkService;
  private final String model;

  public ArkLlmService(
      @Value("${ark.api-key:}") String apiKey,
      @Value("${ark.base-url:https://ark.cn-beijing.volces.com/api/v3}") String baseUrl,
      @Value("${ark.model:doubao-seed-2-0-lite-260428}") String model) {
    String key = resolveArkApiKey(apiKey);
    this.model = model;
    this.arkService = StringUtils.hasText(key)
        ? ArkService.builder().apiKey(key).baseUrl(baseUrl).build()
        : null;
    if (this.arkService == null) {
      log.warn(
          "Ark LLM 未启用：未解析到有效 ARK_API_KEY。可将真实密钥写入 backend/.env.backend.local（推荐，与模板"
              + " .env.backend 分离）、或进程环境变量 / -e ARK_API_KEY；勿使用占位符 __PUT_NEW_KEY_HERE__。"
              + " user.dir={}",
          System.getProperty("user.dir"));
    }
  }

  static boolean isArkPlaceholder(String s) {
    if (!StringUtils.hasText(s)) {
      return true;
    }
    String t = s.trim();
    return "__PUT_NEW_KEY_HERE__".equals(t) || "your-api-key".equals(t);
  }

  /**
   * Spring / 环境变量优先（占位符视为未配置）；其次 {@code .env.backend.local}，再 {@code .env.backend}（与
   * {@code import-backend-env.ps1} 加载顺序一致，且支持 Docker 挂载目录直读文件）。
   */
  static String resolveArkApiKey(String fromSpring) {
    if (StringUtils.hasText(fromSpring) && !isArkPlaceholder(fromSpring)) {
      return fromSpring.trim();
    }
    String fromEnv = System.getenv("ARK_API_KEY");
    if (StringUtils.hasText(fromEnv) && !isArkPlaceholder(fromEnv)) {
      return fromEnv.trim();
    }
    String wd = System.getProperty("user.dir", ".");
    String fromFile = readArkKeyFromEnvBackendFile(Path.of(wd, ".env.backend.local"));
    if (StringUtils.hasText(fromFile)) {
      return fromFile;
    }
    fromFile = readArkKeyFromEnvBackendFile(Path.of(wd, "backend", ".env.backend.local"));
    if (StringUtils.hasText(fromFile)) {
      return fromFile;
    }
    fromFile = readArkKeyFromEnvBackendFile(Path.of(wd, ".env.backend"));
    if (StringUtils.hasText(fromFile)) {
      return fromFile;
    }
    return readArkKeyFromEnvBackendFile(Path.of(wd, "backend", ".env.backend"));
  }

  static String readArkKeyFromEnvBackendFile(Path envFile) {
    if (!Files.isRegularFile(envFile)) {
      return "";
    }
    try {
      for (String raw : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
        String line = raw.trim();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        int eq = line.indexOf('=');
        if (eq < 1) {
          continue;
        }
        String name = line.substring(0, eq).trim();
        if (!"ARK_API_KEY".equals(name)) {
          continue;
        }
        String val = line.substring(eq + 1).trim();
        if (val.length() >= 2 && val.startsWith("\"") && val.endsWith("\"")) {
          val = val.substring(1, val.length() - 1);
        }
        if (!StringUtils.hasText(val)
            || "__PUT_NEW_KEY_HERE__".equals(val)
            || "your-api-key".equals(val)) {
          return "";
        }
        return val;
      }
    } catch (IOException e) {
      LoggerFactory.getLogger(ArkLlmService.class)
          .warn("读取 {} 失败: {}", envFile.toAbsolutePath(), e.getMessage());
    }
    return "";
  }

  public boolean enabled() {
    return arkService != null;
  }

  /** 已配置的模型名（不含密钥），供状态接口展示。 */
  public String getModel() {
    return model;
  }

  public String chat(String prompt) {
    if (!enabled()) {
      return "未配置 ARK_API_KEY，当前返回规则引擎结果。";
    }
    CreateResponsesRequest request = CreateResponsesRequest.builder()
        .model(model)
        .input(ResponsesInput.builder().addListItem(
            ItemEasyMessage.builder().role(ResponsesConstants.MESSAGE_ROLE_USER)
                .content(MessageContent.builder()
                    .addListItem(InputContentItemText.builder().text(prompt).build())
                    .build())
                .build()).build())
        .build();
    ResponseObject resp = arkService.createResponse(request);
    return extractAssistantText(resp);
  }

  static String extractAssistantText(ResponseObject resp) {
    if (resp == null) {
      return "";
    }
    if (resp.getError() != null && StringUtils.hasText(resp.getError().getMessage())) {
      return "API错误: " + resp.getError().getMessage();
    }
    if (resp.getOutput() == null || resp.getOutput().isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (BaseItem item : resp.getOutput()) {
      if (item instanceof ItemOutputMessage iom && iom.getContent() != null) {
        for (OutputContentItem oci : iom.getContent()) {
          if (oci instanceof OutputContentItemText oct && StringUtils.hasText(oct.getText())) {
            sb.append(oct.getText());
          }
        }
      }
    }
    String out = sb.toString().trim();
    return out.isEmpty() ? String.valueOf(resp) : out;
  }

  @PreDestroy
  public void shutdown() {
    if (arkService != null) {
      arkService.shutdownExecutor();
    }
  }
}
