package com.aimap.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "aimap_chat_message")
public class ChatMessageEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "thread_id", nullable = false, length = 96)
  private String threadId;

  @Column(name = "sender_user_type", nullable = false, length = 16)
  private String senderUserType;

  @Column(name = "sender_account", nullable = false, length = 128)
  private String senderAccount;

  @Column(name = "sender_name", nullable = false, length = 128)
  private String senderName = "";

  @Column(nullable = false, columnDefinition = "TEXT")
  private String body;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public String getThreadId() {
    return threadId;
  }

  public void setThreadId(String threadId) {
    this.threadId = threadId;
  }

  public String getSenderUserType() {
    return senderUserType;
  }

  public void setSenderUserType(String senderUserType) {
    this.senderUserType = senderUserType;
  }

  public String getSenderAccount() {
    return senderAccount;
  }

  public void setSenderAccount(String senderAccount) {
    this.senderAccount = senderAccount;
  }

  public String getSenderName() {
    return senderName;
  }

  public void setSenderName(String senderName) {
    this.senderName = senderName;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
