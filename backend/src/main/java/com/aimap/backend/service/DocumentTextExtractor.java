package com.aimap.backend.service;

import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.exception.WriteLimitReachedException;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

@Service
public class DocumentTextExtractor {
  private final AutoDetectParser parser = new AutoDetectParser();
  private final int tikaWriteLimit;
  private final int llmBodyMaxChars;

  public DocumentTextExtractor(
      @Value("${aimap.document.tika-write-limit:500000}") int tikaWriteLimit,
      @Value("${aimap.document.llm-body-max-chars:80000}") int llmBodyMaxChars) {
    this.tikaWriteLimit = Math.max(10_000, tikaWriteLimit);
    this.llmBodyMaxChars = Math.max(2_000, llmBodyMaxChars);
  }

  /** 将抽取正文截断到适合单次大模型请求的体量。 */
  public String truncateForLlm(String plain) {
    if (plain == null || plain.isEmpty()) {
      return "";
    }
    if (plain.length() <= llmBodyMaxChars) {
      return plain;
    }
    return plain.substring(0, llmBodyMaxChars) + "\n\n[正文已超过字数上限，后续已省略]";
  }

  /**
   * 从 PDF / DOC / DOCX 等提取纯文本。加密、损坏或纯扫描无文字层的 PDF 可能得到空串。
   */
  public String extractPlainText(MultipartFile file) {
    Metadata metadata = new Metadata();
    String filename = file.getOriginalFilename();
    if (filename != null && !filename.isBlank()) {
      metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, filename);
    }
    BodyContentHandler handler = new BodyContentHandler(tikaWriteLimit);
    ParseContext ctx = new ParseContext();
    try (InputStream stream = file.getInputStream()) {
      parser.parse(stream, handler, metadata, ctx);
    } catch (IOException e) {
      throw new IllegalArgumentException("读取上传文件失败：" + e.getMessage());
    } catch (SAXException e) {
      if (e instanceof WriteLimitReachedException) {
        throw new IllegalArgumentException("文档过大，已超出单次解析字数上限，请拆分或缩短后重试。");
      }
      Throwable c = e.getCause();
      if (c instanceof WriteLimitReachedException) {
        throw new IllegalArgumentException("文档过大，已超出单次解析字数上限，请拆分或缩短后重试。");
      }
      throw new IllegalArgumentException("文档解析失败，请确认文件格式有效且未损坏。");
    } catch (TikaException e) {
      String m = e.getMessage() == null ? "未知错误" : e.getMessage();
      throw new IllegalArgumentException("文档正文提取失败（可能为加密或本格式不支持）：" + m);
    }
    return normalizeWhitespace(handler.toString());
  }

  private static String normalizeWhitespace(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    String s = raw.replace('\u00a0', ' ').replace('\u200b', ' ').trim();
    s = s.replaceAll(" +", " ");
    s = s.replaceAll("\\n{3,}", "\n\n");
    return s;
  }
}
