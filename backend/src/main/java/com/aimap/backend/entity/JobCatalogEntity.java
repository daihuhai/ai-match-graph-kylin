package com.aimap.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "aimap_job_catalog")
public class JobCatalogEntity {

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
}
