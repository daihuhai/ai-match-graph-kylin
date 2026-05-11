package com.aimap.backend.service;

import com.aimap.backend.auth.BearerUserResolver.LoggedUser;
import com.aimap.backend.entity.CandidateCatalogEntity;
import com.aimap.backend.entity.DocumentEntity;
import com.aimap.backend.entity.JobCatalogEntity;
import com.aimap.backend.entity.UserEntity;
import com.aimap.backend.repo.CandidateCatalogRepository;
import com.aimap.backend.repo.DocumentRepository;
import com.aimap.backend.repo.JobCatalogRepository;
import com.aimap.backend.repo.UserRepository;
import com.aimap.backend.util.JsonHelper;
import com.aimap.backend.util.RiasecMath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InMemoryDataService {

  public record User(String account, String password, String userType) {}

  public record DocumentTask(
      String id,
      String fileName,
      String fileType,
      String docType,
      long startedAt,
      String resultText,
      String resumeCritique,
      Map<String, Integer> resumeHolland,
      String jobCritique,
      Map<String, Integer> jobHolland) {

    public DocumentTask {
      resumeHolland = resumeHolland == null ? Map.of() : Map.copyOf(resumeHolland);
      jobHolland = jobHolland == null ? Map.of() : Map.copyOf(jobHolland);
    }
  }

  public record TalentEntity(
      String recordId,
      String title,
      String org,
      Map<String, Integer> riasec,
      List<String> skills) {}

  private static final Map<String, Integer> DEFAULT_PERSON_HOLLAND =
      Map.of("r", 42, "i", 48, "a", 32, "s", 50, "e", 38, "c", 44);

  private final UserRepository userRepository;
  private final DocumentRepository documentRepository;
  private final JobCatalogRepository jobCatalogRepository;
  private final CandidateCatalogRepository candidateCatalogRepository;

  public InMemoryDataService(
      UserRepository userRepository,
      DocumentRepository documentRepository,
      JobCatalogRepository jobCatalogRepository,
      CandidateCatalogRepository candidateCatalogRepository) {
    this.userRepository = userRepository;
    this.documentRepository = documentRepository;
    this.jobCatalogRepository = jobCatalogRepository;
    this.candidateCatalogRepository = candidateCatalogRepository;
  }

  @Transactional
  public void register(String account, String password, String userType) {
    if (userRepository.existsByUserTypeAndAccount(userType, account)) {
      throw new IllegalArgumentException("账号已存在");
    }
    UserEntity u = new UserEntity();
    u.setUserType(userType);
    u.setAccount(account);
    u.setPassword(password);
    userRepository.save(u);
  }

  @Transactional(readOnly = true)
  public User login(String account, String password, String userType) {
    UserEntity u =
        userRepository
            .findByUserTypeAndAccount(userType, account)
            .orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
    if (!u.getPassword().equals(password)) {
      throw new IllegalArgumentException("账号或密码错误");
    }
    return new User(u.getAccount(), u.getPassword(), u.getUserType());
  }

  @Transactional
  public String createDocTask(
      String fileName,
      String fileType,
      String docType,
      String rawLlmSummary,
      String resumeCritique,
      Map<String, Integer> resumeHolland,
      String jobCritique,
      Map<String, Integer> jobHolland,
      Optional<LoggedUser> uploader) {
    String id = "doc-" + UUID.randomUUID().toString().substring(0, 8);
    DocumentEntity d = new DocumentEntity();
    d.setId(id);
    d.setFileName(fileName);
    d.setFileType(fileType);
    d.setDocType(docType);
    d.setStartedAtMs(Instant.now().toEpochMilli());
    d.setResultText(rawLlmSummary);
    d.setResumeCritique(resumeCritique == null ? "" : resumeCritique);
    d.setJobCritique(jobCritique == null ? "" : jobCritique);
    d.setResumeHollandJson(JsonHelper.riasecToJson(resumeHolland));
    d.setJobHollandJson(JsonHelper.riasecToJson(jobHolland));
    uploader.ifPresent(
        u -> {
          d.setOwnerUserType(u.userType());
          d.setOwnerAccount(u.account());
        });
    documentRepository.save(d);

    uploader.ifPresent(
        u -> {
          userRepository
              .findByUserTypeAndAccount(u.userType(), u.account())
              .ifPresent(
                  ue -> {
                    boolean dirty = false;
                    if ("RESUME".equals(docType)
                        && "PERSON".equals(u.userType())
                        && resumeHolland != null
                        && !resumeHolland.isEmpty()) {
                      ue.setResumeRiasecJson(JsonHelper.riasecToJson(resumeHolland));
                      dirty = true;
                    }
                    if ("JOB_DESC".equals(docType)
                        && "COMPANY".equals(u.userType())
                        && jobHolland != null
                        && !jobHolland.isEmpty()) {
                      ue.setJobRiasecJson(JsonHelper.riasecToJson(jobHolland));
                      dirty = true;
                    }
                    if (dirty) {
                      userRepository.save(ue);
                    }
                  });
        });
    return id;
  }

  @Transactional(readOnly = true)
  public DocumentTask getDocTask(String docId) {
    DocumentEntity d =
        documentRepository
            .findById(docId)
            .orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    return toDocumentTask(d);
  }

  private static DocumentTask toDocumentTask(DocumentEntity d) {
    return new DocumentTask(
        d.getId(),
        d.getFileName(),
        d.getFileType(),
        d.getDocType(),
        d.getStartedAtMs(),
        d.getResultText(),
        d.getResumeCritique() == null ? "" : d.getResumeCritique(),
        JsonHelper.parseRiasec(d.getResumeHollandJson()),
        d.getJobCritique() == null ? "" : d.getJobCritique(),
        JsonHelper.parseRiasec(d.getJobHollandJson()));
  }

  public static String statusByElapsed(long elapsed) {
    if (elapsed < 1_500) {
      return "PENDING";
    }
    if (elapsed < 4_500) {
      return "PROCESSING";
    }
    return "DONE";
  }

  @Transactional(readOnly = true)
  public Map<String, Integer> personHollandForRecommend(Optional<LoggedUser> user) {
    return user
        .filter(u -> "PERSON".equals(u.userType()))
        .flatMap(u -> userRepository.findByUserTypeAndAccount(u.userType(), u.account()))
        .map(UserEntity::getResumeRiasecJson)
        .map(JsonHelper::parseRiasec)
        .filter(m -> !m.isEmpty())
        .orElse(DEFAULT_PERSON_HOLLAND);
  }

  @Transactional(readOnly = true)
  public Map<String, Integer> companyJobHollandForRecommend(Optional<LoggedUser> user) {
    return user
        .filter(u -> "COMPANY".equals(u.userType()))
        .flatMap(u -> userRepository.findByUserTypeAndAccount(u.userType(), u.account()))
        .map(UserEntity::getJobRiasecJson)
        .map(JsonHelper::parseRiasec)
        .filter(m -> !m.isEmpty())
        .orElseGet(this::defaultJobHollandFromDb);
  }

  private Map<String, Integer> defaultJobHollandFromDb() {
    return jobCatalogRepository
        .findById("job-002")
        .map(JobCatalogEntity::getRiasecJson)
        .map(JsonHelper::parseRiasec)
        .filter(m -> !m.isEmpty())
        .orElse(DEFAULT_PERSON_HOLLAND);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> recommendJobs(Optional<LoggedUser> user, int minScore) {
    Map<String, Integer> p = personHollandForRecommend(user);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (JobCatalogEntity j : jobCatalogRepository.findAllByOrderByRecordIdAsc()) {
      TalentEntity t = toTalent(j);
      int score = RiasecMath.similarity100(p, t.riasec());
      if (score >= minScore) {
        rows.add(
            Map.of(
                "recordId", t.recordId(),
                "title", t.title(),
                "org", t.org(),
                "score", score));
      }
    }
    rows.sort(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("score")).reversed());
    return rows;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> recommendCandidates(Optional<LoggedUser> user, int minScore) {
    Map<String, Integer> jobH = companyJobHollandForRecommend(user);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (CandidateCatalogEntity c : candidateCatalogRepository.findAllByOrderByRecordIdAsc()) {
      TalentEntity t = toTalent(c);
      int score = RiasecMath.similarity100(t.riasec(), jobH);
      if (score >= minScore) {
        rows.add(
            Map.of(
                "recordId", t.recordId(),
                "title", t.title(),
                "org", t.org(),
                "score", score));
      }
    }
    rows.sort(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("score")).reversed());
    return rows;
  }

  private static TalentEntity toTalent(JobCatalogEntity j) {
    return new TalentEntity(
        j.getRecordId(),
        j.getTitle(),
        j.getOrg(),
        JsonHelper.parseRiasec(j.getRiasecJson()),
        JsonHelper.parseSkills(j.getSkillsJson()));
  }

  private static TalentEntity toTalent(CandidateCatalogEntity c) {
    return new TalentEntity(
        c.getRecordId(),
        c.getTitle(),
        c.getOrg(),
        JsonHelper.parseRiasec(c.getRiasecJson()),
        JsonHelper.parseSkills(c.getSkillsJson()));
  }

  @Transactional(readOnly = true)
  public Optional<TalentEntity> findJob(String recordId) {
    return jobCatalogRepository.findById(recordId).map(InMemoryDataService::toTalent);
  }

  @Transactional(readOnly = true)
  public Optional<TalentEntity> findCandidate(String recordId) {
    return candidateCatalogRepository.findById(recordId).map(InMemoryDataService::toTalent);
  }

  public Map<String, Object> matchDetailForJob(String jobRecordId, Optional<LoggedUser> personUser) {
    TalentEntity job =
        findJob(jobRecordId).orElseThrow(() -> new IllegalArgumentException("职位记录不存在"));
    Map<String, Integer> person = personHollandForRecommend(personUser);
    return buildDetailMap(jobRecordId, person, job.riasec(), job.skills(), true);
  }

  public Map<String, Object> matchDetailForCandidate(String candRecordId, Optional<LoggedUser> companyUser) {
    TalentEntity cand =
        findCandidate(candRecordId).orElseThrow(() -> new IllegalArgumentException("候选人记录不存在"));
    Map<String, Integer> jobH = companyJobHollandForRecommend(companyUser);
    return buildDetailMap(candRecordId, cand.riasec(), jobH, cand.skills(), false);
  }

  private Map<String, Object> buildDetailMap(
      String recordId,
      Map<String, Integer> person,
      Map<String, Integer> target,
      List<String> skillNames,
      boolean personIsViewer) {
    int similarity = RiasecMath.similarity100(person, target);
    int score = Math.min(100, similarity + 5);
    List<Map<String, Object>> matched =
        skillNames.stream().map(n -> Map.<String, Object>of("name", n, "requiredLevel", 3, "personLevel", 3)).toList();
    return Map.ofEntries(
        Map.entry("recordId", recordId),
        Map.entry("score", score),
        Map.entry(
            "scoreBreakdown",
            Map.of("skillCoverage", 28, "levelMatch", 18, "vectorSim", Math.min(40, similarity / 3), "hollandMatch", similarity / 4)),
        Map.entry("matchedSkills", matched.isEmpty() ? List.of(Map.of("name", "综合素养", "requiredLevel", 2, "personLevel", 2)) : matched),
        Map.entry("missingSkills", List.of(Map.of("name", "可补充与目标岗位强相关的项目经历", "requiredLevel", 2, "gap", 1))),
        Map.entry(
            "rationales",
            List.of(
                "霍兰德六维相似度约 " + similarity + "%，用于衡量人与岗（或岗与人）整体倾向一致性。",
                personIsViewer ? "个人画像取自数据库中最近一次简历解析写入的霍兰德向量。" : "候选人画像来自人才库；岗位画像取自贵司最近一次 JD 解析写入数据库的霍兰德向量。")),
        Map.entry(
            "evidences",
            List.of(
                Map.of("type", "VECTOR", "field", "RIASEC", "snippet", "六维向量比对（R/I/A/S/E/C）", "weight", 0.45),
                Map.of("type", "TEXT", "field", "skills", "snippet", String.join("、", skillNames), "weight", 0.35),
                Map.of("type", "GRAPH", "field", "HOLLAND", "snippet", "霍兰德雷达对齐度", "weight", 0.2))),
        Map.entry("riasec", Map.of("person", person, "target", target, "similarity", similarity)),
        Map.entry("suggestions", List.of("结合解析报告中的改进建议持续优化简历或 JD 表述。", "上传最新版文档可刷新霍兰德估计与匹配结果。")));
  }

  /** Legacy ids rec-job-* / rec-cand-* still supported for old bookmarks. */
  public Map<String, Object> legacyMatchDetail(String recordId) {
    boolean isJob = recordId.startsWith("rec-job-");
    int score = isJob ? (recordId.endsWith("002") ? 79 : 86) : (recordId.endsWith("002") ? 77 : 84);
    Map<String, Integer> person =
        recordId.endsWith("002")
            ? Map.of("r", 30, "i", 55, "a", 35, "s", 40, "e", 25, "c", 60)
            : Map.of("r", 35, "i", 45, "a", 30, "s", 55, "e", 40, "c", 35);
    Map<String, Integer> target =
        isJob
            ? (recordId.endsWith("002")
                ? Map.of("r", 45, "i", 50, "a", 25, "s", 30, "e", 40, "c", 55)
                : Map.of("r", 30, "i", 40, "a", 35, "s", 50, "e", 45, "c", 30))
            : (recordId.endsWith("002")
                ? Map.of("r", 40, "i", 45, "a", 30, "s", 55, "e", 35, "c", 40)
                : Map.of("r", 35, "i", 50, "a", 28, "s", 45, "e", 38, "c", 42));
    int dist = Math.abs(person.get("r") - target.get("r")) + Math.abs(person.get("i") - target.get("i"))
        + Math.abs(person.get("a") - target.get("a")) + Math.abs(person.get("s") - target.get("s"))
        + Math.abs(person.get("e") - target.get("e")) + Math.abs(person.get("c") - target.get("c"));
    int similarity = Math.max(0, Math.min(100, Math.round(100 - dist / 6f)));
    return new LinkedHashMap<>(
        Map.ofEntries(
            Map.entry("recordId", recordId),
            Map.entry("score", score),
            Map.entry("scoreBreakdown", Map.of("skillCoverage", 42, "levelMatch", 20, "vectorSim", 12, "graphReasoning", 12)),
            Map.entry(
                "matchedSkills",
                List.of(
                    Map.of("name", "Vue 3", "requiredLevel", 3, "personLevel", 3),
                    Map.of("name", "TypeScript", "requiredLevel", 3, "personLevel", 2))),
            Map.entry("missingSkills", List.of(Map.of("name", "AntV G6", "requiredLevel", 2, "gap", 2))),
            Map.entry("rationales", List.of("技能覆盖率较高：核心技能匹配，但图谱能力有缺口。", "图谱推理命中：项目经历与技能节点存在关联边。")),
            Map.entry(
                "evidences",
                List.of(
                    Map.of("type", "TEXT", "field", "skills", "snippet", "熟悉 Vue3/TS、Spring Boot、SQL...", "weight", 0.35),
                    Map.of("type", "GRAPH", "field", "HAS_SKILL", "snippet", "Person -> Vue 3 / TypeScript", "weight", 0.25),
                    Map.of("type", "VECTOR", "field", "embedding", "snippet", "语义相似度（文本向量）", "weight", 0.4))),
            Map.entry("riasec", Map.of("person", person, "target", target, "similarity", similarity)),
            Map.entry("suggestions", List.of("补齐图谱可视化能力（G6 基础与交互）", "完善工程化规范：路由守卫、错误兜底与性能分包"))));
  }
}
