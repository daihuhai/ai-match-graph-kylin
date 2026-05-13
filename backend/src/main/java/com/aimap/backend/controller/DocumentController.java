package com.aimap.backend.controller;

import com.aimap.backend.auth.BearerUserResolver;
import com.aimap.backend.auth.BearerUserResolver.LoggedUser;
import com.aimap.backend.common.ApiResponse;
import com.aimap.backend.service.ArkLlmService;
import com.aimap.backend.service.DocumentLlmJsonParser;
import com.aimap.backend.service.DocumentLlmJsonParser.ParsedDoc;
import com.aimap.backend.service.DocumentOriginalStorageService;
import com.aimap.backend.service.DocumentTextExtractor;
import com.aimap.backend.service.InMemoryDataService;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  private final DocumentTextExtractor textExtractor;
  private final DocumentOriginalStorageService originalStorageService;

  public record TalentPoolPublishBody(Boolean publish, String companyAccount) {
    boolean wantsPublish() {
      return publish == null || Boolean.TRUE.equals(publish);
    }
  }

  public DocumentController(
      InMemoryDataService dataService,
      ArkLlmService arkLlmService,
      DocumentLlmJsonParser docParser,
      DocumentTextExtractor textExtractor,
      DocumentOriginalStorageService originalStorageService) {
    this.dataService = dataService;
    this.arkLlmService = arkLlmService;
    this.docParser = docParser;
    this.textExtractor = textExtractor;
    this.originalStorageService = originalStorageService;
  }

  @PostMapping("/upload")
  public ApiResponse<Map<String, String>> upload(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @RequestParam("file") MultipartFile file,
      @RequestParam("docType") String docType,
      @RequestParam(value = "publishToTalentPool", defaultValue = "false") boolean publishToTalentPool,
      @RequestParam(value = "companyAccount", required = false) String companyAccount) {
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

    String plain = textExtractor.extractPlainText(file);
    String bodyForLlm = textExtractor.truncateForLlm(plain);
    String prompt =
        "RESUME".equals(docType)
            ? resumePrompt(name, file.getSize(), bodyForLlm)
            : jobPrompt(name, file.getSize(), bodyForLlm);
    String llmRaw = arkLlmService.chat(prompt);
    ParsedDoc parsed = docParser.parse(llmRaw, "RESUME".equals(docType));

    String resumeCritique = "RESUME".equals(docType) ? parsed.critiqueMarkdown() : "";
    Map<String, Integer> resumeHolland = "RESUME".equals(docType) ? parsed.riasec() : Map.of();
    String jobCritique = "JOB_DESC".equals(docType) ? parsed.critiqueMarkdown() : "";
    Map<String, Integer> jobHolland = "JOB_DESC".equals(docType) ? parsed.riasec() : Map.of();

    List<String> resumeSkillPayload = "RESUME".equals(docType) ? parsed.skills() : List.of();

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
            resumeSkillPayload,
            user);
    originalStorageService.save(docId, name, file);

    user.filter(u -> "JOB_DESC".equals(docType) && "COMPANY".equals(u.userType()))
        .ifPresent(u -> dataService.upsertJobFromCompanyDocument(docId, name, jobHolland, parsed.skills(), u));

    if (publishToTalentPool) {
      LoggedUser u =
          user.orElseThrow(() -> new IllegalArgumentException("需登录后方可将简历加入系统人才库"));
      if (!"RESUME".equals(docType)) {
        throw new IllegalArgumentException("仅简历文档可加入人才库");
      }
      if (!"PERSON".equals(u.userType())) {
        throw new IllegalArgumentException("仅个人端可将简历加入人才库");
      }
      dataService.publishResumeToTalentPool(docId, u, companyAccount);
    }

    return ApiResponse.ok(Map.of("docId", docId));
  }

  @PostMapping("/{id}/talent-pool")
  public ApiResponse<Map<String, Object>> publishTalentPool(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @PathVariable("id") String docId,
      @RequestBody(required = false) TalentPoolPublishBody body) {
    LoggedUser u =
        BearerUserResolver.fromAuthorization(authorization)
            .orElseThrow(() -> new IllegalArgumentException("需登录后方可操作人才库"));
    TalentPoolPublishBody b = body == null ? new TalentPoolPublishBody(true, null) : body;
    if (false && !b.wantsPublish()) {
      throw new IllegalArgumentException("当前仅支持加入人才库（publish=true）");
    }
    if (b.wantsPublish()) {
      dataService.publishResumeToTalentPool(docId, u, b.companyAccount());
    } else {
      dataService.unpublishResumeFromTalentPool(docId, u, b.companyAccount());
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("docId", docId);
    out.put("talentPoolPublished", b.wantsPublish());
    out.put("companyAccount", b.companyAccount());
    return ApiResponse.ok(out);
  }

  @GetMapping("/company-targets")
  public ApiResponse<List<Map<String, String>>> companyTargets() {
    return ApiResponse.ok(
        dataService.listActiveCompanyTargets().stream()
            .map(c -> Map.of("account", c.account()))
            .toList());
  }

  private static String resumePrompt(String fileName, long size, String bodyForLlm) {
    String bodyBlock =
        StringUtils.hasText(bodyForLlm)
            ? """
        以下是由系统自动抽取的简历正文（可能因长度被截断；请严格依据正文与文件名综合判断，勿编造未出现的事实）：
        -----BEGIN_BODY-----
        """
                + bodyForLlm
                + """
        
        -----END_BODY-----
        """
            : """
        正文抽取结果为空（常见于纯扫描件 PDF 无文字层、或加密文档）。请仅结合文件名与大小做保守推断，并在 strengths/weaknesses 中各用一条说明「缺少可解析正文」带来的局限。
        """;

    String head =
        """
        你是资深职业顾问与霍兰德职业兴趣测评专家。用户上传了简历文件。
        文件名："""
            + fileName
            + """
        
        文件大小（字节）："""
            + size
            + """

        """;
    String tail =
        """

        请给出：
        1) 简历优势、不足与可操作的改进建议（需分条、专业、中文；有正文时须引用正文中的经历/技能要点）。
        2) 求职者的霍兰德 RIASEC 六维强度（0-100 整数）：R现实型、I研究型、A艺术型、S社会型、E企业型、C常规型。
        3) 从正文或文件名可合理推断的 3～12 个关键技术词或能力短语（勿编造明显无关项），放入 skills 数组。

        你必须只输出一个 JSON 对象（不要 markdown 代码块、不要前后解释），格式严格如下：
        {"critique":{"strengths":["..."],"weaknesses":["..."],"improvements":["..."]},"riasec":{"R":0,"I":0,"A":0,"S":0,"E":0,"C":0},"skills":["..."]}
        """;
    return head + bodyBlock + tail;
  }

  private static String jobPrompt(String fileName, long size, String bodyForLlm) {
    String bodyBlock =
        StringUtils.hasText(bodyForLlm)
            ? """
        以下是由系统自动抽取的岗位说明正文（可能因长度被截断；请严格依据正文与文件名综合判断，勿编造未出现的事实）：
        -----BEGIN_BODY-----
        """
                + bodyForLlm
                + """
        
        -----END_BODY-----
        """
            : """
        正文抽取结果为空（常见于纯扫描件 PDF 无文字层、或加密文档）。请仅结合文件名与大小做保守推断，并在 summary 中说明「缺少可解析正文」带来的局限。
        """;

    String head =
        """
        你是资深招聘顾问与霍兰德职业兴趣测评专家。企业上传了岗位说明文件。
        文件名："""
            + fileName
            + """
        
        文件大小（字节）："""
            + size
            + """

        """;
    String tail =
        """

        请给出：
        1) 岗位核心要求摘要、关键技能点（中文分条；有正文时须紧扣正文表述）。
        2) 该岗位适合的霍兰德 RIASEC 六维画像（0-100 整数）：R/I/A/S/E/C。

        你必须只输出一个 JSON 对象（不要 markdown 代码块、不要前后解释），格式严格如下：
        {"critique":{"summary":"...","requiredSkills":["..."],"notes":["..."]},"riasec":{"R":0,"I":0,"A":0,"S":0,"E":0,"C":0}}
        """;
    return head + bodyBlock + tail;
  }

  @GetMapping("/{id}/status")
  public ApiResponse<Map<String, String>> status(@PathVariable("id") String docId) {
    InMemoryDataService.DocumentTask task = dataService.getDocTask(docId);
    String status = InMemoryDataService.statusByElapsed(Instant.now().toEpochMilli() - task.startedAt());
    return ApiResponse.ok(Map.of("id", task.id(), "status", status));
  }

  @GetMapping("/{id}/result")
  public ApiResponse<Map<String, Object>> result(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable("id") String docId) {
    InMemoryDataService.DocumentTask task = dataService.getDocTask(docId);
    Optional<LoggedUser> user = BearerUserResolver.fromAuthorization(authorization);
    String status = InMemoryDataService.statusByElapsed(Instant.now().toEpochMilli() - task.startedAt());
    Map<String, Object> resultJson = new LinkedHashMap<>();
    List<String> demoSkills = List.of("Java", "Spring Boot", "Vue 3", "TypeScript", "SQL");
    if ("RESUME".equals(task.docType()) && !task.resumeSkills().isEmpty()) {
      resultJson.put("skills", task.resumeSkills());
    } else {
      resultJson.put("skills", demoSkills);
    }
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

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("docId", task.id());
    out.put("fileName", task.fileName());
    out.put("fileType", task.fileType());
    out.put("status", status);
    out.put("resultJson", resultJson);
    out.put("originalAvailable", originalStorageService.exists(task.id(), task.fileName()));
    out.put(
        "evidences",
        List.of(
            Map.of(
                "field",
                "llm",
                "page",
                1,
                "text",
                "使用 Apache Tika 从上传文件中抽取纯文本后送入大模型；超长正文会截断。纯扫描 PDF（无文字层）或加密文件可能无法抽取正文。")));
    if ("RESUME".equals(task.docType())) {
      boolean canPublish = "PERSON".equals(task.ownerUserType()) && StringUtils.hasText(task.ownerAccount());
      out.put("canPublishToTalentPool", canPublish);
      out.put(
          "deliveredCompanyAccounts",
          user.filter(u -> "PERSON".equals(u.userType()) && u.account().equals(task.ownerAccount()))
              .map(u -> dataService.deliveredCompanyAccounts(docId, u))
              .orElse(List.of()));
    }
    return ApiResponse.ok(out);
  }

  @GetMapping("/{id}/original")
  public ResponseEntity<Resource> original(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
      @PathVariable("id") String docId) {
    InMemoryDataService.DocumentTask task = dataService.getDocTask(docId);
    LoggedUser user =
        BearerUserResolver.fromAuthorization(authorization)
            .orElseThrow(() -> new IllegalArgumentException("请先登录后再查看原始文档"));
    assertCanReadOriginal(task, user);
    Resource resource = originalStorageService.loadAsResource(task.id(), task.fileName());
    MediaType mediaType =
        MediaTypeFactory.getMediaType(task.fileName()).orElse(MediaType.APPLICATION_OCTET_STREAM);
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(task.fileName(), StandardCharsets.UTF_8).build().toString())
        .body(resource);
  }

  private void assertCanReadOriginal(InMemoryDataService.DocumentTask task, LoggedUser user) {
    if ("ADMIN".equals(user.userType())) {
      return;
    }
    boolean ownerMatch =
        user.userType().equals(task.ownerUserType()) && user.account().equals(task.ownerAccount());
    if (ownerMatch) {
      return;
    }
    if ("COMPANY".equals(user.userType())
        && "RESUME".equals(task.docType())
        && dataService.isResumeDeliveredToCompany(task.id(), user.account())) {
      return;
    }
    throw new IllegalArgumentException("当前账号无权查看该原始文档");
  }
}
