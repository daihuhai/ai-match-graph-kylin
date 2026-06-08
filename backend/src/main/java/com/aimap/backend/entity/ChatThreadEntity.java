package com.aimap.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "aimap_chat_thread")
public class ChatThreadEntity {

  @Id
  @Column(length = 96)
  private String id;

  @Column(name = "person_user_type", nullable = false, length = 16)
  private String personUserType = "PERSON";

  @Column(name = "person_account", nullable = false, length = 128)
  private String personAccount;

  @Column(name = "company_user_type", nullable = false, length = 16)
  private String companyUserType = "COMPANY";

  @Column(name = "company_account", nullable = false, length = 128)
  private String companyAccount;

  @Column(name = "context_record_id", nullable = false, length = 64)
  private String contextRecordId;

  @Column(name = "context_title", nullable = false, length = 256)
  private String contextTitle = "";

  @Column(name = "context_org", nullable = false, length = 256)
  private String contextOrg = "";

  @Column(name = "last_message_at")
  private Instant lastMessageAt;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getPersonUserType() {
    return personUserType;
  }

  public void setPersonUserType(String personUserType) {
    this.personUserType = personUserType;
  }

  public String getPersonAccount() {
    return personAccount;
  }

  public void setPersonAccount(String personAccount) {
    this.personAccount = personAccount;
  }

  public String getCompanyUserType() {
    return companyUserType;
  }

  public void setCompanyUserType(String companyUserType) {
    this.companyUserType = companyUserType;
  }

  public String getCompanyAccount() {
    return companyAccount;
  }

  public void setCompanyAccount(String companyAccount) {
    this.companyAccount = companyAccount;
  }

  public String getContextRecordId() {
    return contextRecordId;
  }

  public void setContextRecordId(String contextRecordId) {
    this.contextRecordId = contextRecordId;
  }

  public String getContextTitle() {
    return contextTitle;
  }

  public void setContextTitle(String contextTitle) {
    this.contextTitle = contextTitle;
  }

  public String getContextOrg() {
    return contextOrg;
  }

  public void setContextOrg(String contextOrg) {
    this.contextOrg = contextOrg;
  }

  public Instant getLastMessageAt() {
    return lastMessageAt;
  }

  public void setLastMessageAt(Instant lastMessageAt) {
    this.lastMessageAt = lastMessageAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
