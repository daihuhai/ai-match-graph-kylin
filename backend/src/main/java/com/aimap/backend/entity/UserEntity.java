package com.aimap.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "aimap_user",
    uniqueConstraints = @UniqueConstraint(name = "uk_user_type_account", columnNames = {"user_type", "account"}))
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_type", nullable = false, length = 16)
  private String userType;

  @Column(nullable = false, length = 128)
  private String account;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(name = "resume_riasec_json", columnDefinition = "json")
  private String resumeRiasecJson;

  @Column(name = "job_riasec_json", columnDefinition = "json")
  private String jobRiasecJson;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUserType() {
    return userType;
  }

  public void setUserType(String userType) {
    this.userType = userType;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getResumeRiasecJson() {
    return resumeRiasecJson;
  }

  public void setResumeRiasecJson(String resumeRiasecJson) {
    this.resumeRiasecJson = resumeRiasecJson;
  }

  public String getJobRiasecJson() {
    return jobRiasecJson;
  }

  public void setJobRiasecJson(String jobRiasecJson) {
    this.jobRiasecJson = jobRiasecJson;
  }
}
