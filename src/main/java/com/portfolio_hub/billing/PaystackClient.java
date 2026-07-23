package com.portfolio_hub.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.portfolio_hub.utils.exception.InvalidOperationException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaystackClient {

  private final ObjectMapper mapper;
  private final HttpClient http = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(15))
    .build();
  private final String secretKey;
  private final String baseUrl;

  public PaystackClient(
    ObjectMapper mapper,
    @Value("${application.billing.paystack.secret-key:}") String secretKey,
    @Value(
      "${application.billing.paystack.base-url:https://api.paystack.co}"
    ) String baseUrl
  ) {
    this.mapper = mapper;
    this.secretKey = secretKey;
    this.baseUrl = baseUrl;
  }

  public Initialization initialize(
    String email,
    BigDecimal amount,
    String currency,
    String reference,
    String callbackUrl
  ) {
    requireConfigured();
    try {
      var body = mapper.createObjectNode();
      body.put("email", email);
      body.put("amount", amount.movePointRight(2).longValueExact());
      body.put("currency", currency);
      body.put("reference", reference);
      body.put("callback_url", callbackUrl);
      JsonNode response = send(
        "POST",
        "/transaction/initialize",
        mapper.writeValueAsString(body)
      );
      JsonNode data = response.path("data");
      return new Initialization(
        data.path("authorization_url").asText(),
        data.path("reference").asText(reference)
      );
    } catch (InvalidOperationException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new InvalidOperationException(
        "Paystack checkout could not be started. Try again shortly."
      );
    }
  }

  public Verification verify(String reference) {
    requireConfigured();
    try {
      JsonNode data = send(
        "GET",
        "/transaction/verify/" + reference,
        null
      ).path("data");
      return new Verification(
        "success".equalsIgnoreCase(data.path("status").asText()),
        data.path("reference").asText(),
        BigDecimal.valueOf(data.path("amount").asLong()).movePointLeft(2),
        data.path("currency").asText(),
        data.path("paid_at").asText(null)
      );
    } catch (InvalidOperationException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new InvalidOperationException(
        "Paystack could not verify this payment yet."
      );
    }
  }

  public boolean validSignature(String payload, String signature) {
    if (
      secretKey.isBlank() || signature == null || signature.isBlank()
    ) return false;
    try {
      Mac mac = Mac.getInstance("HmacSHA512");
      mac.init(
        new SecretKeySpec(
          secretKey.getBytes(StandardCharsets.UTF_8),
          "HmacSHA512"
        )
      );
      String expected = HexFormat.of().formatHex(
        mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
      );
      return java.security.MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        signature.getBytes(StandardCharsets.UTF_8)
      );
    } catch (Exception exception) {
      return false;
    }
  }

  public String webhookReference(String payload) {
    try {
      JsonNode root = mapper.readTree(payload);
      return "charge.success".equals(root.path("event").asText())
        ? root.path("data").path("reference").asText(null)
        : null;
    } catch (Exception exception) {
      return null;
    }
  }

  private JsonNode send(String method, String path, String body)
    throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder()
      .uri(URI.create(baseUrl + path))
      .timeout(Duration.ofSeconds(25))
      .header("Authorization", "Bearer " + secretKey)
      .header("Content-Type", "application/json");
    request.method(
      method,
      body == null
        ? HttpRequest.BodyPublishers.noBody()
        : HttpRequest.BodyPublishers.ofString(body)
    );
    HttpResponse<String> response = http.send(
      request.build(),
      HttpResponse.BodyHandlers.ofString()
    );
    JsonNode json = mapper.readTree(response.body());
    if (
      response.statusCode() < 200 ||
      response.statusCode() >= 300 ||
      !json.path("status").asBoolean()
    ) {
      throw new InvalidOperationException(
        json.path("message").asText("Paystack rejected the request.")
      );
    }
    return json;
  }

  private void requireConfigured() {
    if (secretKey.isBlank()) {
      throw new InvalidOperationException(
        "Paystack is not available yet. Choose bank transfer or contact support."
      );
    }
  }

  public record Initialization(String authorizationUrl, String reference) {}

  public record Verification(
    boolean successful,
    String reference,
    BigDecimal amount,
    String currency,
    String paidAt
  ) {}
}
