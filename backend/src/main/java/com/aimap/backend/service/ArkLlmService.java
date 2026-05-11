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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ArkLlmService {
  private final ArkService arkService;
  private final String model;

  public ArkLlmService(
      @Value("${ark.api-key:}") String apiKey,
      @Value("${ark.base-url:https://ark.cn-beijing.volces.com/api/v3}") String baseUrl,
      @Value("${ark.model:doubao-seed-2-0-lite-260428}") String model) {
    String key = StringUtils.hasText(apiKey) ? apiKey : System.getenv("ARK_API_KEY");
    this.model = model;
    this.arkService = StringUtils.hasText(key)
        ? ArkService.builder().apiKey(key).baseUrl(baseUrl).build()
        : null;
  }

  public boolean enabled() {
    return arkService != null;
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
