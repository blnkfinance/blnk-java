package com.blnkfinance.blnk.util;

import com.blnkfinance.blnk.HttpTransport;
import com.blnkfinance.blnk.TransportRequest;
import com.blnkfinance.blnk.TransportResponse;
import com.blnkfinance.blnk.types.BlnkJson;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MultipartBody}: instance detection, multipart encoding
 * of buffer- and file-backed parts (part markers plus the multipart
 * content-type header), and an end-to-end round trip against a local HTTP
 * server.
 */
class MultipartBodyTest {

  @Test
  @DisplayName("isMultipart identifies MultipartBody instances")
  void isMultipartIdentifiesInstances() {
    assertTrue(MultipartBody.isMultipart(MultipartBody.create()));
    assertFalse(MultipartBody.isMultipart(new Object()));
    assertFalse(MultipartBody.isMultipart(BlnkJson.objectNode()));
  }

  @Test
  @DisplayName("encodes buffer-backed parts with a multipart content type")
  void encodesBufferBackedParts() {
    MultipartBody formData = MultipartBody.create();
    formData.append("source", "stripe");
    formData.append("file", "a,b,c".getBytes(StandardCharsets.UTF_8), "test.csv");

    Map<String, String> headers = formData.getHeaders();
    String payload = new String(formData.encode(), StandardCharsets.UTF_8);

    assertTrue(payload.contains("stripe"));
    assertTrue(payload.contains("a,b,c"));
    assertTrue(headers.get("content-type").contains("multipart/form-data"));
  }

  @Test
  @DisplayName("encodes file-backed parts with a multipart content type")
  void encodesFileStreamParts() throws Exception {
    Path dir = Files.createTempDirectory("blnk-formdata-");
    Path filePath = dir.resolve("upload.csv");
    Files.writeString(filePath, "amount,ref\n100,abc");

    MultipartBody formData = MultipartBody.create();
    formData.append("source", "stripe");
    formData.appendFile("file", filePath);

    Map<String, String> headers = formData.getHeaders();
    String payload = new String(formData.encode(), StandardCharsets.UTF_8);

    assertTrue(payload.contains("stripe"));
    assertTrue(payload.contains("amount,ref"));
    assertTrue(payload.contains("100,abc"));
    assertTrue(headers.get("content-type").contains("multipart/form-data"));
  }

  @Test
  @DisplayName("a real HTTP server accepts the encoded multipart body")
  void realHttpServerAcceptsMultipartBody() throws Exception {
    Path dir = Files.createTempDirectory("blnk-multipart-");
    Path filePath = dir.resolve("upload.csv");
    Files.writeString(filePath, "amount,ref\n300,native");

    MultipartBody formData = MultipartBody.create();
    formData.append("source", "stripe");
    formData.appendFile("file", filePath);

    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/upload", exchange -> {
      String payload = new String(exchange.getRequestBody().readAllBytes(),
          StandardCharsets.UTF_8);
      boolean ok = payload.contains("stripe") && payload.contains("300,native");
      byte[] response = ("{\"ok\":" + ok + "}").getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(201, response.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(response);
      }
    });
    server.start();
    try {
      int port = server.getAddress().getPort();
      HttpTransport transport = new HttpTransport();
      TransportRequest request = new TransportRequest(
          "POST", formData.getHeaders(), formData.encode(), true, 5000);

      TransportResponse response =
          transport.execute("http://127.0.0.1:" + port + "/upload", request);

      assertEquals(201, response.status());
      assertTrue(HttpClientUtils.readResponseJsonBody(response).get("ok").asBoolean());
    } finally {
      server.stop(0);
    }
  }
}
