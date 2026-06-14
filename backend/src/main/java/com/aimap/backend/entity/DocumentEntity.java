package com.aimap.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "aimap_document")
public class DocumentEntity {

  @Id
  @Column(length = 40)
  private String id;

  @Column(name = "file_name", nullable = false, length = 512)
  private String fileName;

  @Column(name = "file_type", nullable = false, length = 16)
  private String fileType;

  @Column(name = "doc_type", nullable = false, length = 32)
  private String docType;

  @Column(name = "started_at_ms", nullable = false)
  private long startedAtMs;

  @Column(name = "result_text", columnDefinition = "text")
  private String resultText;

  @Column(name = "resume_critique", columnDefinition = "text")
  private String resumeCritique;

  @Column(name = "job_critique", columnDefinition = "text")
  private String jobCritique;

  @Column(name = "resume_holland_json", columnDefinition = "json")
  private String resumeHollandJson;

  @Column(name = "resume_skills_json", columnDefinition = "json")
  private String resumeSkillsJson;

  @Column(name = "resume_education_json", columnDefinition = "json")
  private String resumeEducationJson;

  @Column(name = "job_holland_json", columnDefinition = "json")
  private String jobHollandJson;

  @Column(name = "owner_user_type", length = 16)
  private String ownerUserType;

  @Column(name = "owner_account", length = 128)
  private String ownerAccount;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getDocType() {
    return docType;
  }

  public void setDocType(String docType) {
    this.docType = docType;
  }

  public long getStartedAtMs() {
    return startedAtMs;
  }

  public void setStartedAtMs(long startedAtMs) {
    this.startedAtMs = startedAtMs;
  }

  public String getResultText() {
    return resultText;
  }

  public void setResultText(String resultText) {
    this.resultText = resultText;
  }

  public String getResumeCritique() {
    return resumeCritique;
  }

  public void setResumeCritique(String resumeCritique) {
    this.resumeCritique = resumeCritique;
  }

  public String getJobCritique() {
    return jobCritique;
  }

  public void setJobCritique(String jobCritique) {
    this.jobCritique = jobCritique;
  }

  public String getResumeHollandJson() {
    return resumeHollandJson;
  }

  public void setResumeHollandJson(String resumeHollandJson) {
    this.resumeHollandJson = resumeHollandJson;
  }

  public String getResumeSkillsJson() {
    return resumeSkillsJson;
  }

  public void setResumeSkillsJson(String resumeSkillsJson) {
    this.resumeSkillsJson = resumeSkillsJson;
  }

  public String getResumeEducationJson() {
    return resumeEducationJson;
  }

  public void setResumeEducationJson(String resumeEducationJson) {
    this.resumeEducationJson = resumeEducationJson;
  }

  public String getJobHollandJson() {
    return jobHollandJson;
  }

  public void setJobHollandJson(String jobHollandJson) {
    this.jobHollandJson = jobHollandJson;
  }

  public String getOwnerUserType() {
    return ownerUserType;
  }

  public void setOwnerUserType(String ownerUserType) {
    this.ownerUserType = ownerUserType;
  }

  public String getOwnerAccount() {
    return ownerAccount;
  }

  public void setOwnerAccount(String ownerAccount) {
    this.ownerAccount = ownerAccount;
  }
}
