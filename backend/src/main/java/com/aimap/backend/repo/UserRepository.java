package com.aimap.backend.repo;

import com.aimap.backend.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
  List<UserEntity> findAllByOrderByIdAsc();

  List<UserEntity> findByUserTypeAndStatusOrderByAccountAsc(String userType, String status);

  boolean existsByUserTypeAndAccount(String userType, String account);

  boolean existsByUserTypeAndPhone(String userType, String phone);

  boolean existsByUserTypeAndEmail(String userType, String email);

  boolean existsByUserTypeAndAccountAndIdNot(String userType, String account, Long id);

  boolean existsByUserTypeAndPhoneAndIdNot(String userType, String phone, Long id);

  boolean existsByUserTypeAndEmailAndIdNot(String userType, String email, Long id);

  Optional<UserEntity> findByUserTypeAndAccount(String userType, String account);

  Optional<UserEntity> findByUserTypeAndPhone(String userType, String phone);

  Optional<UserEntity> findByUserTypeAndEmail(String userType, String email);
}
