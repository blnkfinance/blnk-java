package com.blnkfinance.blnk.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The multipart form body used for file uploads. Wire behavior:
 * <ul>
 *   <li>boundary = 26 dashes + 24 random digits;</li>
 *   <li>{@code getHeaders()} → {@code {content-type: multipart/form-data; boundary=...}}
 *       — these headers override everything else on the request;</li>
 *   <li>CRLF part framing with {@code Content-Disposition: form-data; name="..."}
 *       (plus {@code filename} and a mime-guessed {@code Content-Type} for file
 *       parts).</li>
 * </ul>
 * Multipart requests never set the JSON content-type and are never retried.
 */
public final class MultipartBody {

  private record Part(String name, String filename, String contentType, byte[] content) {}

  private final String boundary;
  private final List<Part> parts = new ArrayList<>();

  private MultipartBody() {
    StringBuilder sb = new StringBuilder("--------------------------");
    for (int i = 0; i < 24; i++) {
      sb.append(ThreadLocalRandom.current().nextInt(10));
    }
    this.boundary = sb.toString();
  }

  public static MultipartBody create() {
    return new MultipartBody();
  }

  /** True when {@code data} is a multipart form body. */
  public static boolean isMultipart(Object data) {
    return data instanceof MultipartBody;
  }

  /** Appends a simple string field; string parts carry no Content-Type line. */
  public MultipartBody append(String name, String value) {
    parts.add(new Part(name, null, null, value.getBytes(StandardCharsets.UTF_8)));
    return this;
  }

  /** Appends a buffer-backed file part with an explicit filename. */
  public MultipartBody append(String name, byte[] content, String filename) {
    parts.add(new Part(name, filename, guessContentType(filename), content));
    return this;
  }

  /**
   * Appends a file part read from disk; the filename is derived from the
   * path's last segment.
   */
  public MultipartBody appendFile(String name, Path file) {
    try {
      byte[] content = Files.readAllBytes(file);
      String filename = file.getFileName() == null ? "file" : file.getFileName().toString();
      parts.add(new Part(name, filename, guessContentType(filename), content));
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  /** Appends a file part from a stream (fully read) with an explicit filename. */
  public MultipartBody appendStream(String name, InputStream stream, String filename) {
    try {
      byte[] content = stream.readAllBytes();
      parts.add(new Part(name, filename, guessContentType(filename), content));
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
  }

  public String boundary() {
    return boundary;
  }

  /** Headers to merge into the request: the multipart content type carrying this body's boundary. */
  public Map<String, String> getHeaders() {
    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("content-type", "multipart/form-data; boundary=" + boundary);
    return headers;
  }

  /** Encodes the full multipart payload. */
  public byte[] encode() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      for (Part part : parts) {
        StringBuilder head = new StringBuilder();
        head.append("--").append(boundary).append("\r\n");
        head.append("Content-Disposition: form-data; name=\"").append(part.name()).append('"');
        if (part.filename() != null) {
          head.append("; filename=\"").append(part.filename()).append('"');
        }
        head.append("\r\n");
        if (part.contentType() != null) {
          head.append("Content-Type: ").append(part.contentType()).append("\r\n");
        }
        head.append("\r\n");
        out.write(head.toString().getBytes(StandardCharsets.UTF_8));
        out.write(part.content());
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
      }
      out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e.getMessage(), e);
    }
    return out.toByteArray();
  }

  private static String guessContentType(String filename) {
    if (filename == null) {
      return "application/octet-stream";
    }
    String lower = filename.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".csv")) {
      return "text/csv";
    }
    if (lower.endsWith(".json")) {
      return "application/json";
    }
    if (lower.endsWith(".txt")) {
      return "text/plain";
    }
    return "application/octet-stream";
  }
}
