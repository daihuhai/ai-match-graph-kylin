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
    name = "aimap_chat_participant",
    uniqueConstraints = @UniqueConstraint(name = "uk_chat_participant_user", columnNames = {"thread_id", "user_type", "account"}))
public class ChatParticipantEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "thread_id", nullable = false, length = 96)
  private String threadId;

  @Column(name = "user_type", nullable = false, length = 16)
  private String userType;

  @Column(nullable = false, length = 128)
  private String account;

  @Column(name = "display_name", nullable = false, length = 128)
  private String displayName = "";

  @Column(name = "last_read_at")
  private Instant lastReadAt;

  @Column(name = "last_read_message_id")
  private Long lastReadMessageId;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;

  public Long getId() {
    return id;
  }

  public String getThreadId() {
    return threadId;
  }

  public void setThreadId(String threadId) {
    this.threadId = threadId;
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

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Instant getLastReadAt() {
    return lastReadAt;
  }

  public void setLastReadAt(Instant lastReadAt) {
    this.lastReadAt = lastReadAt;
  }

  public Long getLastReadMessageId() {
    return lastReadMessageId;
  }

  public void setLastReadMessageId(Long lastReadMessageId) {
    this.lastReadMessageId = lastReadMessageId;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
