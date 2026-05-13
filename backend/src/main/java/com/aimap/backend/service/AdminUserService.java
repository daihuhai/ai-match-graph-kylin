package com.aimap.backend.service;

import com.aimap.backend.entity.UserEntity;
import com.aimap.backend.repo.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserService {
  public record AdminUserRow(
      Long id,
      String account,
      String userType,
      String status,
      String phone,
      String email,
      Instant createdAt,
      Instant lastLoginAt) {}

  public record SaveUserReq(
      Long id,
      String account,
      String userType,
      String status,
      String phone,
      String email) {}

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public List<AdminUserRow> list() {
    return userRepository.findAllByOrderByIdAsc().stream().map(this::toRow).toList();
  }

  @Transactional
  public AdminUserRow save(SaveUserReq req) {
    String account = normalizeRequired(req.account(), "账号不能为空");
    String userType = normalizeRequired(req.userType(), "角色不能为空");
    String status = normalizeStatus(req.status());
    String phone = normalizeNullable(req.phone());
    String email = normalizeNullable(req.email());

    UserEntity user =
        req.id() == null
            ? new UserEntity()
            : userRepository.findById(req.id()).orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    validateUnique(req.id(), userType, account, phone, email);

    user.setAccount(account);
    user.setUserType(userType);
    user.setStatus(status);
    user.setPhone(phone);
    user.setEmail(email);
    if (req.id() == null) {
      user.setPassword(passwordEncoder.encode("123456"));
    }

    return toRow(userRepository.save(user));
  }

  @Transactional
  public AdminUserRow setStatus(Long id, String status) {
    UserEntity user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    user.setStatus(normalizeStatus(status));
    return toRow(userRepository.save(user));
  }

  @Transactional
  public void delete(Long id) {
    if (!userRepository.existsById(id)) {
      throw new IllegalArgumentException("用户不存在");
    }
    userRepository.deleteById(id);
  }

  @Transactional
  public void resetPassword(Long id) {
    UserEntity user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    user.setPassword(passwordEncoder.encode("123456"));
    userRepository.save(user);
  }

  private void validateUnique(Long id, String userType, String account, String phone, String email) {
    boolean accountExists =
        id == null
            ? userRepository.existsByUserTypeAndAccount(userType, account)
            : userRepository.existsByUserTypeAndAccountAndIdNot(userType, account, id);
    if (accountExists) {
      throw new IllegalArgumentException("同角色下账号已存在");
    }
    if (StringUtils.hasText(phone)) {
      boolean phoneExists =
          id == null
              ? userRepository.existsByUserTypeAndPhone(userType, phone)
              : userRepository.existsByUserTypeAndPhoneAndIdNot(userType, phone, id);
      if (phoneExists) {
        throw new IllegalArgumentException("同角色下手机号已存在");
      }
    }
    if (StringUtils.hasText(email)) {
      boolean emailExists =
          id == null
              ? userRepository.existsByUserTypeAndEmail(userType, email)
              : userRepository.existsByUserTypeAndEmailAndIdNot(userType, email, id);
      if (emailExists) {
        throw new IllegalArgumentException("同角色下邮箱已存在");
      }
    }
  }

  private AdminUserRow toRow(UserEntity u) {
    return new AdminUserRow(
        u.getId(),
        u.getAccount(),
        u.getUserType(),
        StringUtils.hasText(u.getStatus()) ? u.getStatus() : "ACTIVE",
        u.getPhone(),
        u.getEmail(),
        u.getCreatedAt(),
        u.getLastLoginAt());
  }

  private static String normalizeRequired(String value, String message) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }

  private static String normalizeNullable(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private static String normalizeStatus(String status) {
    String value = normalizeRequired(status, "状态不能为空").toUpperCase();
    if (!"ACTIVE".equals(value) && !"DISABLED".equals(value)) {
      throw new IllegalArgumentException("状态不合法");
    }
    return value;
  }
}
