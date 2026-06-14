package com.aimap.backend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentOriginalStorageService {
  private final Path rootDir;

  public DocumentOriginalStorageService(
      @Value("${aimap.document.storage-root:./data/document-originals}") String storageRoot) {
    this.rootDir = Path.of(storageRoot).toAbsolutePath().normalize();
  }

  public void save(String docId, String fileName, MultipartFile file) {
    try {
      Files.createDirectories(rootDir);
      try (InputStream in = file.getInputStream()) {
        Files.copy(in, resolvePath(docId, fileName), StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException e) {
      throw new IllegalStateException("保存原始文档失败: " + e.getMessage(), e);
    }
  }

  public boolean exists(String docId, String fileName) {
    return Files.isRegularFile(resolvePath(docId, fileName));
  }

  public Resource loadAsResource(String docId, String fileName) {
    Path path = resolvePath(docId, fileName);
    if (!Files.isRegularFile(path)) {
      throw new IllegalArgumentException("原始文档不存在");
    }
    return new FileSystemResource(path);
  }

  public void delete(String docId) {
    try {
      for (String ext : List.of(".pdf", ".doc", ".docx", ".bin")) {
        Path path = rootDir.resolve(docId + ext).normalize();
        Files.deleteIfExists(path);
      }
    } catch (IOException e) {
      throw new IllegalStateException("删除原始文档失败: " + e.getMessage(), e);
    }
  }

  private Path resolvePath(String docId, String fileName) {
    String ext = extensionOf(fileName);
    return rootDir.resolve(docId + ext).normalize();
  }

  private static String extensionOf(String fileName) {
    if (!StringUtils.hasText(fileName)) {
      return ".bin";
    }
    int dot = fileName.lastIndexOf('.');
    if (dot < 0 || dot == fileName.length() - 1) {
      return ".bin";
    }
    String ext = fileName.substring(dot).trim().toLowerCase();
    return ext.matches("\\.[a-z0-9]{1,10}") ? ext : ".bin";
  }
}
