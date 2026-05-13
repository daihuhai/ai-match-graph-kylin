package com.aimap.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "aimap_talent_pool")
public class TalentPoolEntity {

  @Id
  @Column(name = "record_id", length = 32)
  private String recordId;

  @Column(nullable = false, length = 256)
  private String title;

  @Column(nullable = false, length = 256)
  private String org;

  @Column(name = "riasec_json", nullable = false, columnDefinition = "json")
  private String riasecJson;

  @Column(name = "skills_json", nullable = false, columnDefinition = "json")
  private String skillsJson;

  @Column(name = "source", nullable = false, length = 16)
  private String source = "SEED";

  @Column(name = "document_id", length = 40)
  private String documentId;

  @Column(name = "owner_user_type", length = 16)
  private String ownerUserType;

  @Column(name = "owner_account", length = 128)
  private String ownerAccount;

  @Column(name = "target_company_user_type", length = 16)
  private String targetCompanyUserType;

  @Column(name = "target_company_account", length = 128)
  private String targetCompanyAccount;

  @Column(name = "in_pool", nullable = false)
  private boolean inPool = true;

  public String getRecordId() {
    return recordId;
  }

  public void setRecordId(String recordId) {
    this.recordId = recordId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getOrg() {
    return org;
  }

  public void setOrg(String org) {
    this.org = org;
  }

  public String getRiasecJson() {
    return riasecJson;
  }

  public void setRiasecJson(String riasecJson) {
    this.riasecJson = riasecJson;
  }

  public String getSkillsJson() {
    return skillsJson;
  }

  public void setSkillsJson(String skillsJson) {
    this.skillsJson = skillsJson;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getDocumentId() {
    return documentId;
  }

  public void setDocumentId(String documentId) {
    this.documentId = documentId;
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

  public boolean isInPool() {
    return inPool;
  }

  public void setInPool(boolean inPool) {
    this.inPool = inPool;
  }

  public String getTargetCompanyUserType() {
    return targetCompanyUserType;
  }

  public void setTargetCompanyUserType(String targetCompanyUserType) {
    this.targetCompanyUserType = targetCompanyUserType;
  }

  public String getTargetCompanyAccount() {
    return targetCompanyAccount;
  }

  public void setTargetCompanyAccount(String targetCompanyAccount) {
    this.targetCompanyAccount = targetCompanyAccount;
  }
}
