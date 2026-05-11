package com.aimap.backend.controller;

import com.aimap.backend.auth.BearerUserResolver;
import com.aimap.backend.auth.BearerUserResolver.LoggedUser;
import com.aimap.backend.common.ApiResponse;
import com.aimap.backend.service.ArkLlmService;
import com.aimap.backend.service.DocumentLlmJsonParser;
import com.aimap.backend.service.DocumentLlmJsonParser.ParsedDoc;
import com.aimap.backend.service.InMemoryDataService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/document")
public class DocumentController {
  private final InMemoryDataService dataService;
  private final ArkLlmService arkLlmService;
  private final DocumentLlmJsonParser docParser;

  public DocumentController(
      InMemoryDataService dataService, ArkLlmService arkLlmService, DocumentLlmJsonParser docParser) {
    this.dataService = dataService;
    this.arkLlmService = arkLlmService;
    this.docParser = docParser;
  }

  @PostMapping("/upload")
  public ApiResponse<Map<String, String>> upload(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestParam("file") MultipartFile file,
      @RequestParam("docType") String docType) {
    String name = file.getOriginalFilename();
    if (!StringUtils.hasText(name)) {
      throw new IllegalArgumentException("文件名不能为空");
    }
    String lower = name.toLowerCase();
    if (!(lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx"))) {
      throw new IllegalArgumentException("仅支持 PDF/DOC/DOCX");
    }
    String fileType = lower.endsWith(".pdf") ? "PDF" : "DOC";
    Optional<LoggedUser> user = BearerUserResolver.fromAuthorization(authorization);

    String prompt =
        "RESUME".equals(docType) ? resumePrompt(name, file.getSize()) : jobPrompt(name, file.getSize());
    String llmRaw = arkLlmService.chat(prompt);
    ParsedDoc parsed = docParser.parse(llmRaw, "RESUME".equals(docType));

    String resumeCritique = "RESUME".equals(docType) ? parsed.critiqueMarkdown() : "";
    Map<String, Integer> resumeHolland = "RESUME".equals(docType) ? parsed.riasec() : Map.of();
    String jobCritique = "JOB_DESC".equals(docType) ? parsed.critiqueMarkdown() : "";
    Map<String, Integer> jobHolland = "JOB_DESC".equals(docType) ? parsed.riasec() : Map.of();

    String docId =
        dataService.createDocTask(
            name,
            fileType,
            docType,
            llmRaw,
            resumeCritique,
            resumeHolland,
            jobCritique,
            jobHolland,
            user);
    return ApiResponse.ok(Map.of("docId", docId));
  }

  private static String resumePrompt(String fileName, long size) {
    return """
        你是资深职业顾问与霍兰德职业兴趣测评专家。用户上传了简历文件（当前系统无法读取正文，仅知文件名与字节大小）。
        文件名：%s
        文件大小（字节）：%d

        请基于文件名中常见信息（岗位意向、年限、技术栈关键词等）给出：
        1) 简历优势、不足与可操作的改进建议（需分条、专业、中文）。
        2) 估计求职者的霍兰德 RIASEC 六维强度（0-100 整数）：R现实型、I研究型、A艺术型、S社会型、E企业型、C常规型。

        你必须只输出一个 JSON 对象（不要 markdown 代码块、不要前后解释），格式严格如下：
        {"critique":{"strengths":["..."],"weaknesses":["..."],"improvements":["..."]},"riasec":{"R":0,"I":0,"A":0,"S":0,"E":0,"C":0}}
        """
        .formatted(fileName, size);
  }

  private static String jobPrompt(String fileName, long size) {
    return """
        你是资深招聘顾问与霍兰德职业兴趣测评专家。企业上传了岗位说明文件（当前系统无法读取正文，仅知文件名与字节大小）。
        文件名：%s
        文件大小（字节）：%d

        请基于文件名中常见信息（岗位名称、级别、方向关键词等）给出：
        1) 岗位核心要求摘要、关键技能点（中文分条）。
        2) 该岗位适合的霍兰德 RIASEC 六维画像（0-100 整数）：R/I/A/S/E/C。

        你必须只输出一个 JSON 对象（不要 markdown 代码块、不要前后解释），格式严格如下：
        {"critique":{"summary":"...","requiredSkills":["..."],"notes":["..."]},"riasec":{"R":0,"I":0,"A":0,"S":0,"E":0,"C":0}}
        """
        .formatted(fileName, size);
  }

  @GetMapping("/{id}/status")
  public ApiResponse<Map<String, String>> status(@PathVariable("id") String docId) {
    InMemoryDataService.DocumentTask task = dataService.getDocTask(docId);
    String status = InMemoryDataService.statusByElapsed(Instant.now().toEpochMilli() - task.startedAt());
    return ApiResponse.ok(Map.of("id", task.id(), "status", status));
  }

  @GetMapping("/{id}/result")
  public ApiResponse<Map<String, Object>> result(@PathVariable("id") String docId) {
    InMemoryDataService.DocumentTask task = dataService.getDocTask(docId);
    String status = InMemoryDataService.statusByElapsed(Instant.now().toEpochMilli() - task.startedAt());
    Map<String, Object> resultJson = new LinkedHashMap<>();
    resultJson.put("skills", List.of("Java", "Spring Boot", "Vue 3", "TypeScript", "SQL"));
    resultJson.put(
        "education", List.of(Map.of("school", "某高校", "degree", "本科", "major", "计算机科学与技术")));
    resultJson.put(
        "projects",
        List.of(Map.of("name", "能力图谱系统", "summary", task.resultText() == null ? "" : task.resultText())));
    if ("RESUME".equals(task.docType())) {
      resultJson.put("resumeCritique", task.resumeCritique());
      resultJson.put("hollandRiasec", task.resumeHolland());
      resultJson.put("llmRaw", task.resultText());
    } else {
      resultJson.put("jobCritique", task.jobCritique());
      resultJson.put("jobHollandRiasec", task.jobHolland());
      resultJson.put("llmRaw", task.resultText());
    }
    return ApiResponse.ok(
        Map.of(
            "docId", task.id(),
            "status", status,
            "resultJson", resultJson,
            "evidences",
            List.of(
                Map.of(
                    "field",
                    "llm",
                    "page",
                    1,
                    "text",
                    "基于文件名与大小的模型推断；接入正文解析后可显著提升准确度。"))));
  }
}
