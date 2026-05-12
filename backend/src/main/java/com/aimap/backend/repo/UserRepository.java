package com.aimap.backend.repo;

import com.aimap.backend.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  boolean existsByUserTypeAndAccount(String userType, String account);

  boolean existsByUserTypeAndPhone(String userType, String phone);

  boolean existsByUserTypeAndEmail(String userType, String email);

  Optional<UserEntity> findByUserTypeAndAccount(String userType, String account);

  Optional<UserEntity> findByUserTypeAndPhone(String userType, String phone);

  Optional<UserEntity> findByUserTypeAndEmail(String userType, String email);
}
