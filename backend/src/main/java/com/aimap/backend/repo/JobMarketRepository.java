package com.aimap.backend.repo;

import com.aimap.backend.entity.JobMarketEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobMarketRepository extends JpaRepository<JobMarketEntity, String> {

  List<JobMarketEntity> findAllByOrderByRecordIdAsc();

  Optional<JobMarketEntity> findByDocumentId(String documentId);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM JobMarketEntity j WHERE j.documentId = :docId")
  void deleteByDocumentId(@Param("docId") String docId);
}
