package com.aimap.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

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

  @Column(length = 32)
  private String phone;

  @Column(length = 255)
  private String email;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(nullable = false, length = 16)
  private String status = "ACTIVE";

  @Column(name = "resume_riasec_json", columnDefinition = "json")
  private String resumeRiasecJson;

  @Column(name = "job_riasec_json", columnDefinition = "json")
  private String jobRiasecJson;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

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

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastLoginAt() {
    return lastLoginAt;
  }

  public void setLastLoginAt(Instant lastLoginAt) {
    this.lastLoginAt = lastLoginAt;
  }
}
