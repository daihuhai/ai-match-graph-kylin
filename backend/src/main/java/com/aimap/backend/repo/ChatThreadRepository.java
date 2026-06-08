package com.aimap.backend.repo;

import com.aimap.backend.entity.ChatThreadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadRepository extends JpaRepository<ChatThreadEntity, String> {}
