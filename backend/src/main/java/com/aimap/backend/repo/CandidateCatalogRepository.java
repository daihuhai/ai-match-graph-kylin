package com.aimap.backend.repo;

import com.aimap.backend.entity.CandidateCatalogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateCatalogRepository extends JpaRepository<CandidateCatalogEntity, String> {

  List<CandidateCatalogEntity> findAllByOrderByRecordIdAsc();
}
