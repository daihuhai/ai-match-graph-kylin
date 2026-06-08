package com.aimap.backend.service;

import com.aimap.backend.auth.BearerUserResolver.LoggedUser;
import com.aimap.backend.entity.ChatMessageEntity;
import com.aimap.backend.entity.ChatParticipantEntity;
import com.aimap.backend.entity.ChatThreadEntity;
import com.aimap.backend.repo.ChatMessageRepository;
import com.aimap.backend.repo.ChatParticipantRepository;
import com.aimap.backend.repo.ChatThreadRepository;
import com.aimap.backend.repo.UserRepository;
import com.aimap.backend.util.ChatThreadIds;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChatService {

  private final ChatThreadRepository threadRepository;
  private final ChatParticipantRepository participantRepository;
  private final ChatMessageRepository messageRepository;
  private final UserRepository userRepository;

  public ChatService(
      ChatThreadRepository threadRepository,
      ChatParticipantRepository participantRepository,
      ChatMessageRepository messageRepository,
      UserRepository userRepository) {
    this.threadRepository = threadRepository;
    this.participantRepository = participantRepository;
    this.messageRepository = messageRepository;
    this.userRepository = userRepository;
  }

  public LoggedUser requireChatUser(Optional<LoggedUser> user) {
    LoggedUser u = user.orElseThrow(() -> new IllegalArgumentException("请先登录"));
    if ("ADMIN".equals(u.userType())) {
      throw new IllegalArgumentException("管理员账号不支持消息中心");
    }
    if (!"PERSON".equals(u.userType()) && !"COMPANY".equals(u.userType())) {
      throw new IllegalArgumentException("当前账号类型不支持消息");
    }
    return u;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listThreads(LoggedUser user) {
    List<ChatParticipantEntity> inbox =
        participantRepository.findByUserTypeAndAccountOrderByUpdatedAtDesc(user.userType(), user.account());
    List<Map<String, Object>> out = new ArrayList<>();
    for (ChatParticipantEntity mine : inbox) {
      ChatThreadEntity thread =
          threadRepository.findById(mine.getThreadId()).orElse(null);
      if (thread == null) {
        continue;
      }
      out.add(toThreadView(thread, mine, user));
    }
    out.sort(
        (a, b) ->
            String.valueOf(b.get("updatedAt")).compareTo(String.valueOf(a.get("updatedAt"))));
    return out;
  }

  @Transactional
  public Map<String, Object> ensureThread(
      LoggedUser user,
      String personAccount,
      String personName,
      String companyAccount,
      String companyName,
      String contextRecordId,
      String contextTitle,
      String contextOrg) {
    ChatThreadIds.requireNonBlank(personAccount, "personAccount");
    ChatThreadIds.requireNonBlank(companyAccount, "companyAccount");
    ChatThreadIds.requireNonBlank(contextRecordId, "contextRecordId");

    String person = personAccount.trim();
    String company = companyAccount.trim();
    String context = contextRecordId.trim();

    if ("PERSON".equals(user.userType()) && !person.equals(user.account())) {
      throw new IllegalArgumentException("个人用户只能以自己的账号发起会话");
    }
    if ("COMPANY".equals(user.userType()) && !company.equals(user.account())) {
      throw new IllegalArgumentException("企业用户只能以自己的账号发起会话");
    }

    assertActiveUser("PERSON", person);
    assertActiveUser("COMPANY", company);

    String threadId = ChatThreadIds.buildId(person, company, context);
    ChatThreadEntity thread =
        threadRepository
            .findById(threadId)
            .orElseGet(
                () -> {
                  ChatThreadEntity t = new ChatThreadEntity();
                  t.setId(threadId);
                  t.setPersonAccount(person);
                  t.setCompanyAccount(company);
                  t.setContextRecordId(context);
                  t.setContextTitle(trimOrEmpty(contextTitle));
                  t.setContextOrg(trimOrEmpty(contextOrg));
                  return threadRepository.save(t);
                });

    if (StringUtils.hasText(contextTitle)) {
      thread.setContextTitle(trimOrEmpty(contextTitle));
    }
    if (StringUtils.hasText(contextOrg)) {
      thread.setContextOrg(trimOrEmpty(contextOrg));
    }
    threadRepository.save(thread);

    ensureParticipant(threadId, "PERSON", person, trimOrEmpty(personName, person));
    ensureParticipant(threadId, "COMPANY", company, trimOrEmpty(companyName, company));

    ChatParticipantEntity mine =
        participantRepository
            .findByThreadIdAndUserTypeAndAccount(threadId, user.userType(), user.account())
            .orElseThrow();
    return toThreadView(thread, mine, user);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> listMessages(LoggedUser user, String threadId) {
    ChatParticipantEntity mine = requireParticipant(threadId, user);
    return messageRepository.findByThreadIdOrderByIdAsc(threadId).stream()
        .map(m -> toMessageView(m, mine))
        .toList();
  }

  @Transactional
  public Map<String, Object> sendMessage(LoggedUser user, String threadId, String text) {
    if (!StringUtils.hasText(text)) {
      throw new IllegalArgumentException("消息内容不能为空");
    }
    ChatParticipantEntity mine = requireParticipant(threadId, user);
    String body = text.trim();
    if (body.length() > 4000) {
      throw new IllegalArgumentException("消息内容过长（最多 4000 字）");
    }

    ChatMessageEntity msg = new ChatMessageEntity();
    msg.setThreadId(threadId);
    msg.setSenderUserType(user.userType());
    msg.setSenderAccount(user.account());
    msg.setSenderName(mine.getDisplayName());
    msg.setBody(body);
    messageRepository.save(msg);

    Instant now = msg.getCreatedAt() != null ? msg.getCreatedAt() : Instant.now();
    threadRepository
        .findById(threadId)
        .ifPresent(
            t -> {
              t.setLastMessageAt(now);
              threadRepository.save(t);
            });

    for (ChatParticipantEntity p : participantRepository.findByThreadId(threadId)) {
      if (p.getId().equals(mine.getId())) {
        p.setLastReadAt(now);
        p.setLastReadMessageId(msg.getId());
      }
      participantRepository.save(p);
    }

    return toMessageView(msg, mine);
  }

  @Transactional
  public void markRead(LoggedUser user, String threadId, Long lastMessageId) {
    ChatParticipantEntity mine = requireParticipant(threadId, user);
    long watermark =
        lastMessageId != null
            ? lastMessageId
            : messageRepository.findByThreadIdOrderByIdAsc(threadId).stream()
                .reduce((a, b) -> b)
                .map(ChatMessageEntity::getId)
                .orElse(0L);
    mine.setLastReadMessageId(watermark);
    mine.setLastReadAt(Instant.now());
    participantRepository.save(mine);
  }

  private void assertActiveUser(String userType, String account) {
    userRepository
        .findByUserTypeAndAccount(userType, account)
        .filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus()))
        .orElseThrow(() -> new IllegalArgumentException("对端用户不存在或已禁用：" + account));
  }

  private void ensureParticipant(String threadId, String userType, String account, String displayName) {
    participantRepository
        .findByThreadIdAndUserTypeAndAccount(threadId, userType, account)
        .ifPresentOrElse(
            existing -> {
              if (StringUtils.hasText(displayName) && !displayName.equals(existing.getDisplayName())) {
                existing.setDisplayName(displayName);
                participantRepository.save(existing);
              }
            },
            () -> {
              ChatParticipantEntity p = new ChatParticipantEntity();
              p.setThreadId(threadId);
              p.setUserType(userType);
              p.setAccount(account);
              p.setDisplayName(displayName);
              participantRepository.save(p);
            });
  }

  private ChatParticipantEntity requireParticipant(String threadId, LoggedUser user) {
    if (!StringUtils.hasText(threadId)) {
      throw new IllegalArgumentException("threadId 不能为空");
    }
    return participantRepository
        .findByThreadIdAndUserTypeAndAccount(threadId, user.userType(), user.account())
        .orElseThrow(() -> new IllegalArgumentException("无权访问该会话"));
  }

  private Map<String, Object> toThreadView(
      ChatThreadEntity thread, ChatParticipantEntity mine, LoggedUser user) {
    String personName = personDisplayName(thread.getId(), thread.getPersonAccount());
    String companyName = companyDisplayName(thread.getId(), thread.getCompanyAccount());
    long unread =
        messageRepository.countUnread(
            thread.getId(), mine.getLastReadMessageId(), user.userType(), user.account());
    Instant updated = thread.getLastMessageAt() != null ? thread.getLastMessageAt() : thread.getUpdatedAt();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", thread.getId());
    m.put("personAccount", thread.getPersonAccount());
    m.put("companyAccount", thread.getCompanyAccount());
    m.put("personName", personName);
    m.put("companyName", companyName);
    m.put("contextRecordId", thread.getContextRecordId());
    m.put("contextTitle", thread.getContextTitle());
    m.put("contextOrg", thread.getContextOrg());
    m.put("updatedAt", updated != null ? updated.toString() : Instant.now().toString());
    m.put("unreadCount", unread);
    return m;
  }

  private String personDisplayName(String threadId, String personAccount) {
    return participantRepository
        .findByThreadIdAndUserTypeAndAccount(threadId, "PERSON", personAccount)
        .map(ChatParticipantEntity::getDisplayName)
        .filter(StringUtils::hasText)
        .orElse(personAccount);
  }

  private String companyDisplayName(String threadId, String companyAccount) {
    return participantRepository
        .findByThreadIdAndUserTypeAndAccount(threadId, "COMPANY", companyAccount)
        .map(ChatParticipantEntity::getDisplayName)
        .filter(StringUtils::hasText)
        .orElse(companyAccount);
  }

  private Map<String, Object> toMessageView(ChatMessageEntity msg, ChatParticipantEntity mine) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", String.valueOf(msg.getId()));
    m.put("threadId", msg.getThreadId());
    m.put("senderRole", msg.getSenderUserType());
    m.put("senderAccount", msg.getSenderAccount());
    m.put("senderName", msg.getSenderName());
    m.put("text", msg.getBody());
    m.put(
        "createdAt",
        msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : Instant.now().toString());
    List<String> readBy = new ArrayList<>();
    readBy.add(msg.getSenderUserType() + ":" + msg.getSenderAccount().toLowerCase());
    Long lastRead = mine.getLastReadMessageId();
    if (lastRead != null && msg.getId() != null && msg.getId() <= lastRead) {
      readBy.add(mine.getUserType() + ":" + mine.getAccount().toLowerCase());
    }
    m.put("readBy", readBy);
    return m;
  }

  private static String trimOrEmpty(String s) {
    return s == null ? "" : s.trim();
  }

  private static String trimOrEmpty(String s, String fallback) {
    String t = trimOrEmpty(s);
    return StringUtils.hasText(t) ? t : fallback;
  }
}
