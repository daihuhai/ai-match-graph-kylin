package com.aimap.backend.repo;

import com.aimap.backend.entity.ChatParticipantEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipantEntity, Long> {

  Optional<ChatParticipantEntity> findByThreadIdAndUserTypeAndAccount(
      String threadId, String userType, String account);

  List<ChatParticipantEntity> findByUserTypeAndAccountOrderByUpdatedAtDesc(String userType, String account);

  List<ChatParticipantEntity> findByThreadId(String threadId);
}
