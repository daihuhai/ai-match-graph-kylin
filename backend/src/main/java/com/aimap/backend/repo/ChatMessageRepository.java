package com.aimap.backend.repo;

import com.aimap.backend.entity.ChatMessageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

  List<ChatMessageEntity> findByThreadIdOrderByIdAsc(String threadId);

  @Query(
      """
      SELECT COUNT(m) FROM ChatMessageEntity m
      WHERE m.threadId = :threadId
        AND m.id > COALESCE(:lastReadId, 0)
        AND NOT (m.senderUserType = :userType AND m.senderAccount = :account)
      """)
  long countUnread(
      @Param("threadId") String threadId,
      @Param("lastReadId") Long lastReadId,
      @Param("userType") String userType,
      @Param("account") String account);
}
