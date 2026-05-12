package com.aimap.backend.repo;

import com.aimap.backend.entity.TalentPoolEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TalentPoolRepository extends JpaRepository<TalentPoolEntity, String> {

  List<TalentPoolEntity> findByInPoolIsTrueOrderByRecordIdAsc();

  Optional<TalentPoolEntity> findByDocumentId(String documentId);
}
