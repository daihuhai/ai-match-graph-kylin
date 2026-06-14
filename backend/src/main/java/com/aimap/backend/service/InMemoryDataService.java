package com.aimap.backend.service;

import com.aimap.backend.auth.BearerUserResolver.LoggedUser;
import com.aimap.backend.entity.DocumentEntity;
import com.aimap.backend.entity.JobMarketEntity;
import com.aimap.backend.entity.TalentPoolEntity;
import com.aimap.backend.entity.UserEntity;
import com.aimap.backend.repo.DocumentRepository;
import com.aimap.backend.repo.JobMarketRepository;
import com.aimap.backend.repo.TalentPoolRepository;
import com.aimap.backend.repo.UserRepository;
import com.aimap.backend.util.JsonHelper;
import com.aimap.backend.util.RiasecMath;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
      Map<String, Integer> jobHolland,
      String ownerUserType,
      String ownerAccount,
      List<String> resumeSkills,
      List<Map<String, String>> resumeEducation) {

    public DocumentTask {
      resumeHolland = resumeHolland == null ? Map.of() : Map.copyOf(resumeHolland);
      jobHolland = jobHolland == null ? Map.of() : Map.copyOf(jobHolland);
      resumeSkills = resumeSkills == null ? List.of() : List.copyOf(resumeSkills);
      resumeEducation = resumeEducation == null ? List.of() : List.copyOf(resumeEducation);
    }
  }

  public record TalentEntity(
      String recordId,
      String title,
      String org,
      Map<String, Integer> riasec,
      List<String> skills) {}

  public record CompanyTarget(String account) {}

  private final UserRepository userRepository;
  private final DocumentRepository documentRepository;
  private final JobMarketRepository jobMarketRepository;
  private final TalentPoolRepository talentPoolRepository;
  private final PasswordEncoder passwordEncoder;

  public InMemoryDataService(
      UserRepository userRepository,
      DocumentRepository documentRepository,
      JobMarketRepository jobMarketRepository,
      TalentPoolRepository talentPoolRepository,
      PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.documentRepository = documentRepository;
    this.jobMarketRepository = jobMarketRepository;
    this.talentPoolRepository = talentPoolRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public void register(String account, String password, String userType, String phone, String email) {
    if (userRepository.existsByUserTypeAndAccount(userType, account)) {
      throw new IllegalArgumentException("账号已存在");
    }
    String p = normalizeNullable(phone);
    String e = normalizeNullable(email);
    if (StringUtils.hasText(p) && userRepository.existsByUserTypeAndPhone(userType, p)) {
      throw new IllegalArgumentException("该手机号已被注册");
    }
    if (StringUtils.hasText(e) && userRepository.existsByUserTypeAndEmail(userType, e)) {
      throw new IllegalArgumentException("该邮箱已被注册");
    }
    UserEntity u = new UserEntity();
    u.setUserType(userType);
    u.setAccount(account);
    u.setPhone(p);
    u.setEmail(e);
    u.setPassword(passwordEncoder.encode(password));
    u.setStatus("ACTIVE");
    userRepository.save(u);
  }

  @Transactional
  public User login(String loginId, String password, String userType) {
    UserEntity u =
        findUserForLogin(userType, loginId).orElseThrow(() -> new IllegalArgumentException("账号或密码错误"));
    if (!passwordMatches(u.getPassword(), password)) {
      throw new IllegalArgumentException("账号或密码错误");
    }
    if (!"ACTIVE".equalsIgnoreCase(u.getStatus())) {
      throw new IllegalArgumentException("账号已被禁用");
    }
    u.setLastLoginAt(Instant.now());
    userRepository.save(u);
    return new User(u.getAccount(), u.getPassword(), u.getUserType());
  }

  private Optional<UserEntity> findUserForLogin(String userType, String loginId) {
    if (!StringUtils.hasText(loginId)) {
      return Optional.empty();
    }
    String id = loginId.trim();
    Optional<UserEntity> u = userRepository.findByUserTypeAndAccount(userType, id);
    if (u.isEmpty()) {
      u = userRepository.findByUserTypeAndPhone(userType, id);
    }
    if (u.isEmpty()) {
      u = userRepository.findByUserTypeAndEmail(userType, id);
    }
    return u;
  }

  private boolean passwordMatches(String stored, String raw) {
    if (!StringUtils.hasText(stored)) {
      return false;
    }
    if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
      return passwordEncoder.matches(raw, stored);
    }
    return stored.equals(raw);
  }

  private static String normalizeNullable(String s) {
    if (!StringUtils.hasText(s)) {
      return null;
    }
    return s.trim();
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
      List<String> resumeSkills,
      List<Map<String, String>> resumeEducation,
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
    d.setResumeSkillsJson(JsonHelper.skillsToJson(resumeSkills));
    d.setResumeEducationJson(JsonHelper.educationToJson(resumeEducation));
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
        JsonHelper.parseRiasec(d.getJobHollandJson()),
        d.getOwnerUserType(),
        d.getOwnerAccount(),
        JsonHelper.parseSkills(d.getResumeSkillsJson()),
        JsonHelper.parseEducation(d.getResumeEducationJson()));
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
        .orElse(Map.of("r", 42, "i", 48, "a", 32, "s", 50, "e", 38, "c", 44));
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
    return jobMarketRepository
        .findById("job-002")
        .map(JobMarketEntity::getRiasecJson)
        .map(JsonHelper::parseRiasec)
        .filter(m -> !m.isEmpty())
        .orElse(Map.of("r", 42, "i", 48, "a", 32, "s", 50, "e", 38, "c", 44));
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> recommendJobsForHolland(Map<String, Integer> personHolland, int minScore) {
    Map<String, Integer> p = personHolland == null || personHolland.isEmpty() ? Map.of("r", 42, "i", 48, "a", 32, "s", 50, "e", 38, "c", 44) : personHolland;
    List<Map<String, Object>> rows = new ArrayList<>();
    for (JobMarketEntity j : jobMarketRepository.findAllByOrderByRecordIdAsc()) {
      TalentEntity t = toTalent(j);
      int score = RiasecMath.similarity100(p, t.riasec());
      if (score >= minScore) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("recordId", t.recordId());
        item.put("title", t.title());
        item.put("org", t.org());
        item.put("companyAccount", j.getOwnerAccount() == null ? "" : j.getOwnerAccount());
        item.put("score", score);
        rows.add(item);
      }
    }
    rows.sort(Comparator.comparingInt((Map<String, Object> m) -> (Integer) m.get("score")).reversed());
    return rows;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> recommendJobs(Optional<LoggedUser> user, int minScore) {
    return recommendJobsForHolland(personHollandForRecommend(user), minScore);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> recommendJobsForResumeDocument(String docId, int minScore) {
    DocumentEntity d =
        documentRepository.findById(docId).orElseThrow(() -> new IllegalArgumentException("文档不存在"));
    if (!"RESUME".equals(d.getDocType())) {
      throw new IllegalArgumentException("仅简历文档可计算与人才市场的岗位匹配");
    }
    Map<String, Integer> p = JsonHelper.parseRiasec(d.getResumeHollandJson());
    return recommendJobsForHolland(p, minScore);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> recommendCandidates(Optional<LoggedUser> user, int minScore) {
    LoggedUser companyUser =
        user.filter(u -> "COMPANY".equals(u.userType()))
            .orElseThrow(() -> new IllegalArgumentException("仅企业账号可查看本企业人才库"));
    Map<String, Integer> jobH = companyJobHollandForRecommend(user);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (TalentPoolEntity c :
        talentPoolRepository.findByTargetCompanyUserTypeAndTargetCompanyAccountAndInPoolIsTrueOrderByRecordIdAsc(
            "COMPANY", companyUser.account())) {
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

  private static TalentEntity toTalent(JobMarketEntity j) {
    return new TalentEntity(
        j.getRecordId(),
        j.getTitle(),
        j.getOrg(),
        JsonHelper.parseRiasec(j.getRiasecJson()),
        JsonHelper.parseSkills(j.getSkillsJson()));
  }

  private static TalentEntity toTalent(TalentPoolEntity c) {
    return new TalentEntity(
        c.getRecordId(),
        c.getTitle(),
        c.getOrg(),
        JsonHelper.parseRiasec(c.getRiasecJson()),
        JsonHelper.parseSkills(c.getSkillsJson()));
  }

  @Transactional(readOnly = true)
  public Optional<TalentEntity> findJob(String recordId) {
    return jobMarketRepository.findById(recordId).map(InMemoryDataService::toTalent);
  }

  @Transactional(readOnly = true)
  public Optional<TalentEntity> findCandidate(String recordId) {
    return talentPoolRepository.findById(recordId).map(InMemoryDataService::toTalent);
  }

  @Transactional(readOnly = true)
  public List<CompanyTarget> listActiveCompanyTargets() {
    return userRepository.findByUserTypeAndStatusOrderByAccountAsc("COMPANY", "ACTIVE").stream()
        .map(UserEntity::getAccount)
        .filter(StringUtils::hasText)
        .distinct()
        .map(CompanyTarget::new)
        .toList();
  }

  @Transactional
  public void upsertJobFromCompanyDocument(
      String docId, String fileName, Map<String, Integer> jobHolland, List<String> skills, LoggedUser owner) {
    if (!StringUtils.hasText(docId) || jobHolland == null || jobHolland.isEmpty()) {
      return;
    }
    jobMarketRepository.deleteByDocumentId(docId);
    JobMarketEntity j = new JobMarketEntity();
    j.setRecordId(jobRecordIdFromDocId(docId));
    j.setTitle(trimFileBaseName(fileName, 240));
    j.setOrg("人才市场 · " + owner.account());
    j.setRiasecJson(JsonHelper.riasecToJson(jobHolland));
    j.setSkillsJson(JsonHelper.skillsToJson(skills));
    j.setSource("COMPANY_DOC");
    j.setDocumentId(docId);
    j.setOwnerUserType(owner.userType());
    j.setOwnerAccount(owner.account());
    jobMarketRepository.save(j);
  }

  @Transactional
  public void publishResumeToTalentPool(String docId, LoggedUser u, String companyAccount) {
    DocumentEntity d =
        documentRepository.findById(docId).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    assertDocOwnedByPerson(d, u);
    String normalizedCompany = normalizeCompanyAccount(companyAccount);
    Map<String, Integer> h = JsonHelper.parseRiasec(d.getResumeHollandJson());
    if (h.isEmpty()) {
      throw new IllegalArgumentException("尚未生成简历霍兰德数据，无法加入人才库");
    }
    List<String> skills = JsonHelper.parseSkills(d.getResumeSkillsJson());
    String rid = talentRecordIdFromDocId(docId, normalizedCompany);
    TalentPoolEntity row =
        talentPoolRepository
            .findByDocumentIdAndTargetCompanyUserTypeAndTargetCompanyAccount(docId, "COMPANY", normalizedCompany)
            .orElseGet(TalentPoolEntity::new);
    row.setRecordId(rid);
    row.setTitle(buildTalentTitle(d.getFileName()));
    row.setOrg("人才库 · " + u.account());
    row.setRiasecJson(JsonHelper.riasecToJson(h));
    row.setSkillsJson(JsonHelper.skillsToJson(skills));
    row.setSource("USER_DOC");
    row.setDocumentId(docId);
    row.setOwnerUserType("PERSON");
    row.setOwnerAccount(u.account());
    row.setTargetCompanyUserType("COMPANY");
    row.setTargetCompanyAccount(normalizedCompany);
    row.setInPool(true);
    talentPoolRepository.save(row);
  }

  @Transactional
  public void unpublishResumeFromTalentPool(String docId, LoggedUser u, String companyAccount) {
    DocumentEntity d =
        documentRepository.findById(docId).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    assertDocOwnedByPerson(d, u);
    String normalizedCompany = normalizeCompanyAccount(companyAccount);
    TalentPoolEntity row =
        talentPoolRepository
            .findByDocumentIdAndTargetCompanyUserTypeAndTargetCompanyAccount(docId, "COMPANY", normalizedCompany)
            .orElseThrow(() -> new IllegalArgumentException("该简历版本尚未投递到目标企业"));
    row.setInPool(false);
    talentPoolRepository.save(row);
  }

  @Transactional(readOnly = true)
  public List<String> deliveredCompanyAccounts(String docId, LoggedUser personUser) {
    DocumentEntity d =
        documentRepository.findById(docId).orElseThrow(() -> new IllegalArgumentException("文档不存在"));
    assertDocOwnedByPerson(d, personUser);
    LinkedHashSet<String> out = new LinkedHashSet<>();
    for (TalentPoolEntity row :
        talentPoolRepository.findByDocumentIdAndOwnerUserTypeAndOwnerAccountAndInPoolIsTrueOrderByTargetCompanyAccountAsc(
            docId, "PERSON", personUser.account())) {
      if (StringUtils.hasText(row.getTargetCompanyAccount())) {
        out.add(row.getTargetCompanyAccount());
      }
    }
    return List.copyOf(out);
  }

  @Transactional(readOnly = true)
  public boolean isResumeDeliveredToCompany(String docId, String companyAccount) {
    if (!StringUtils.hasText(companyAccount)) {
      return false;
    }
    return talentPoolRepository
        .findByDocumentIdAndTargetCompanyUserTypeAndTargetCompanyAccount(docId, "COMPANY", companyAccount.trim())
        .filter(TalentPoolEntity::isInPool)
        .isPresent();
  }

  private static void assertDocOwnedByPerson(DocumentEntity d, LoggedUser u) {
    if (!"RESUME".equals(d.getDocType())) {
      throw new IllegalArgumentException("仅简历可加入人才库");
    }
    if (!"PERSON".equals(u.userType())) {
      throw new IllegalArgumentException("仅个人用户可加入人才库");
    }
    if (!StringUtils.hasText(d.getOwnerAccount()) || !d.getOwnerAccount().equals(u.account())) {
      throw new IllegalArgumentException("只能为自己的简历文档执行该操作");
    }
  }

  @Transactional
  public void deleteDocument(String docId, LoggedUser user) {
    DocumentEntity d =
        documentRepository.findById(docId).orElseThrow(() -> new IllegalArgumentException("文档不存在"));
    if (!"ADMIN".equals(user.userType())) {
      boolean ownerMatch =
          user.userType().equals(d.getOwnerUserType()) && user.account().equals(d.getOwnerAccount());
      if (!ownerMatch) {
        throw new IllegalArgumentException("无权限删除该文档");
      }
    }
    talentPoolRepository.deleteByDocumentId(docId);
    jobMarketRepository.deleteByDocumentId(docId);
    documentRepository.deleteById(docId);
  }

  private static String jobRecordIdFromDocId(String docId) {
    return "jm-" + docId.substring(4);
  }

  private static String talentRecordIdFromDocId(String docId, String companyAccount) {
    return "tp-" + docId.substring(4) + "-" + Integer.toUnsignedString(companyAccount.hashCode(), 36);
  }

  private static String trimFileBaseName(String fileName, int maxLen) {
    if (!StringUtils.hasText(fileName)) {
      return "岗位说明";
    }
    int dot = fileName.lastIndexOf('.');
    String base = dot > 0 ? fileName.substring(0, dot) : fileName;
    base = base.trim();
    if (base.length() > maxLen) {
      base = base.substring(0, maxLen);
    }
    return base.isEmpty() ? "岗位说明" : base;
  }

  private static String buildTalentTitle(String fileName) {
    String base = trimFileBaseName(fileName, 220);
    return "候选人：" + base;
  }

  private String normalizeCompanyAccount(String companyAccount) {
    if (!StringUtils.hasText(companyAccount)) {
      throw new IllegalArgumentException("请选择投递目标企业");
    }
    String normalized = companyAccount.trim();
    UserEntity company =
        userRepository
            .findByUserTypeAndAccount("COMPANY", normalized)
            .orElseThrow(() -> new IllegalArgumentException("目标企业不存在"));
    if (!"ACTIVE".equalsIgnoreCase(company.getStatus())) {
      throw new IllegalArgumentException("目标企业当前不可投递");
    }
    return company.getAccount();
  }

  public Map<String, Object> matchDetailForJob(String jobRecordId, Optional<LoggedUser> personUser) {
    TalentEntity job =
        findJob(jobRecordId).orElseThrow(() -> new IllegalArgumentException("职位记录不存在"));
    JobMarketEntity row =
        jobMarketRepository.findById(jobRecordId).orElseThrow(() -> new IllegalArgumentException("职位记录不存在"));
    Map<String, Integer> person = personHollandForRecommend(personUser);
    Map<String, Object> out = new LinkedHashMap<>(buildDetailMap(jobRecordId, person, job.riasec(), job.skills(), true));
    out.put("jobCompanyAccount", row.getOwnerAccount() == null ? "" : row.getOwnerAccount());
    out.put("jobTitle", row.getTitle());
    out.put("jobOrg", row.getOrg());
    return out;
  }

  public Map<String, Object> matchDetailForCandidate(String candRecordId, Optional<LoggedUser> companyUser) {
    LoggedUser viewer =
        companyUser.filter(u -> "COMPANY".equals(u.userType()))
            .orElseThrow(() -> new IllegalArgumentException("仅企业账号可查看人才详情"));
    TalentPoolEntity row =
        talentPoolRepository.findById(candRecordId).orElseThrow(() -> new IllegalArgumentException("人才库记录不存在"));
    if (!viewer.account().equals(row.getTargetCompanyAccount())) {
      throw new IllegalArgumentException("当前企业无权查看该人才记录");
    }
    TalentEntity cand =
        findCandidate(candRecordId).orElseThrow(() -> new IllegalArgumentException("人才库记录不存在"));
    Map<String, Integer> jobH = companyJobHollandForRecommend(companyUser);
    Map<String, Object> out = new LinkedHashMap<>(buildDetailMap(candRecordId, cand.riasec(), jobH, cand.skills(), false));
    out.put("candidateDocumentId", row.getDocumentId());
    out.put("candidateAccount", row.getOwnerAccount() == null ? "" : row.getOwnerAccount());
    out.put("candidateTitle", row.getTitle());
    out.put("candidateOrg", row.getOrg());
    out.put("jobCompanyAccount", row.getTargetCompanyAccount() == null ? "" : row.getTargetCompanyAccount());
    return out;
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
                personIsViewer
                    ? "个人画像取自数据库中最近一次简历解析写入的霍兰德向量。"
                    : "候选人画像来自系统人才库；岗位画像取自贵司最近一次 JD 解析写入数据库的霍兰德向量。")),
        Map.entry(
            "evidences",
            List.of(
                Map.of("type", "VECTOR", "field", "RIASEC", "snippet", "六维向量比对（R/I/A/S/E/C）", "weight", 0.45),
                Map.of("type", "TEXT", "field", "skills", "snippet", String.join("、", skillNames), "weight", 0.35),
                Map.of("type", "GRAPH", "field", "HOLLAND", "snippet", "霍兰德雷达对齐度", "weight", 0.2))),
        Map.entry("riasec", Map.of("person", person, "target", target, "similarity", similarity)),
        Map.entry("suggestions", List.of("结合解析报告中的改进建议持续优化简历或 JD 表述。", "上传最新版文档可刷新霍兰德估计与匹配结果。")));
  }
}
