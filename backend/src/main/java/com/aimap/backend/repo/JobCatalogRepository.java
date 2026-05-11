package com.aimap.backend.repo;

import com.aimap.backend.entity.JobCatalogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobCatalogRepository extends JpaRepository<JobCatalogEntity, String> {

  List<JobCatalogEntity> findAllByOrderByRecordIdAsc();
}
