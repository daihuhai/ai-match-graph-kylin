package com.aimap.backend.repo;

import com.aimap.backend.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  boolean existsByUserTypeAndAccount(String userType, String account);

  Optional<UserEntity> findByUserTypeAndAccount(String userType, String account);
}
