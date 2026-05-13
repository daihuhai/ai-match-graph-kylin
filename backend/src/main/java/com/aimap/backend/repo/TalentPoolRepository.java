package com.aimap.backend.repo;

import com.aimap.backend.entity.TalentPoolEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TalentPoolRepository extends JpaRepository<TalentPoolEntity, String> {

  List<TalentPoolEntity> findByInPoolIsTrueOrderByRecordIdAsc();

  Optional<TalentPoolEntity> findByDocumentId(String documentId);

  List<TalentPoolEntity> findByTargetCompanyUserTypeAndTargetCompanyAccountAndInPoolIsTrueOrderByRecordIdAsc(
      String targetCompanyUserType, String targetCompanyAccount);

  Optional<TalentPoolEntity> findByDocumentIdAndTargetCompanyUserTypeAndTargetCompanyAccount(
      String documentId, String targetCompanyUserType, String targetCompanyAccount);

  List<TalentPoolEntity> findByDocumentIdAndOwnerUserTypeAndOwnerAccountAndInPoolIsTrueOrderByTargetCompanyAccountAsc(
      String documentId, String ownerUserType, String ownerAccount);
}
