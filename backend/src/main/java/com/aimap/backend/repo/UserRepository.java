package com.aimap.backend.repo;

import com.aimap.backend.entity.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
  List<UserEntity> findAllByOrderByIdAsc();

  List<UserEntity> findByUserTypeAndStatusOrderByAccountAsc(String userType, String status);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM aimap_user WHERE user_type = :userType AND account = :account)",
      nativeQuery = true)
  boolean existsByUserTypeAndAccount(
      @Param("userType") String userType, @Param("account") String account);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM aimap_user WHERE user_type = :userType AND phone = :phone)",
      nativeQuery = true)
  boolean existsByUserTypeAndPhone(@Param("userType") String userType, @Param("phone") String phone);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM aimap_user WHERE user_type = :userType AND email = :email)",
      nativeQuery = true)
  boolean existsByUserTypeAndEmail(@Param("userType") String userType, @Param("email") String email);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM aimap_user WHERE user_type = :userType AND account = :account AND id <> :id)",
      nativeQuery = true)
  boolean existsByUserTypeAndAccountAndIdNot(
      @Param("userType") String userType, @Param("account") String account, @Param("id") Long id);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM aimap_user WHERE user_type = :userType AND phone = :phone AND id <> :id)",
      nativeQuery = true)
  boolean existsByUserTypeAndPhoneAndIdNot(
      @Param("userType") String userType, @Param("phone") String phone, @Param("id") Long id);

  @Query(
      value =
          "SELECT EXISTS (SELECT 1 FROM aimap_user WHERE user_type = :userType AND email = :email AND id <> :id)",
      nativeQuery = true)
  boolean existsByUserTypeAndEmailAndIdNot(
      @Param("userType") String userType, @Param("email") String email, @Param("id") Long id);

  Optional<UserEntity> findByUserTypeAndAccount(String userType, String account);

  Optional<UserEntity> findByUserTypeAndPhone(String userType, String phone);

  Optional<UserEntity> findByUserTypeAndEmail(String userType, String email);
}
