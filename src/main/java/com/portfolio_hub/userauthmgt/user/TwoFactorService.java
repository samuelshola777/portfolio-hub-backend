package com.portfolio_hub.userauthmgt.user;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorService {

  private final SecureRandom secureRandom = new SecureRandom();

  public String createSecret() {
    byte[] bytes = new byte[20];
    secureRandom.nextBytes(bytes);
    return new Base32().encodeToString(bytes).replace("=", "");
  }

  public String createQrDataUrl(String secret, String email) {
    try {
      String uri =
        "otpauth://totp/Portfolio%20Hub:" +
        encode(email) +
        "?secret=" +
        secret +
        "&issuer=Portfolio%20Hub&digits=6&period=30";
      BitMatrix matrix = new QRCodeWriter().encode(
        uri,
        BarcodeFormat.QR_CODE,
        256,
        256
      );
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", output);
      return (
        "data:image/png;base64," +
        Base64.getEncoder().encodeToString(output.toByteArray())
      );
    } catch (Exception exception) {
      throw new IllegalStateException(
        "Unable to create authenticator QR code",
        exception
      );
    }
  }

  public boolean verify(String secret, String token) {
    if (
      secret == null ||
      token == null ||
      !token.replace(" ", "").matches("\\d{6}")
    ) return false;
    long counter = System.currentTimeMillis() / 1000 / 30;
    for (long offset = -1; offset <= 1; offset++) {
      if (
        generateCode(secret, counter + offset).equals(token.replace(" ", ""))
      ) return true;
    }
    return false;
  }

  public List<String> createRecoveryCodes() {
    List<String> codes = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      byte[] bytes = new byte[8];
      secureRandom.nextBytes(bytes);
      String raw = HexFormat.of().formatHex(bytes).toUpperCase();
      codes.add(raw.substring(0, 8) + "-" + raw.substring(8));
    }
    return codes;
  }

  public String hashRecoveryCodes(List<String> codes) {
    return String.join(",", codes.stream().map(this::hash).toList());
  }

  public boolean useRecoveryCode(User user, String code) {
    if (user.getRecoveryCodeHashes() == null || code == null) return false;
    List<String> hashes = new ArrayList<>(
      List.of(user.getRecoveryCodeHashes().split(","))
    );
    String wanted = hash(code.trim().toUpperCase().replace(" ", ""));
    boolean removed = hashes.remove(wanted);
    if (removed) user.setRecoveryCodeHashes(String.join(",", hashes));
    return removed;
  }

  private String generateCode(String secret, long counter) {
    try {
      byte[] key = new Base32().decode(secret);
      byte[] data = ByteBuffer.allocate(8).putLong(counter).array();
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(data);
      int offset = hash[hash.length - 1] & 0x0f;
      int binary =
        ((hash[offset] & 0x7f) << 24) |
        ((hash[offset + 1] & 0xff) << 16) |
        ((hash[offset + 2] & 0xff) << 8) |
        (hash[offset + 3] & 0xff);
      return String.format("%06d", binary % 1_000_000);
    } catch (Exception exception) {
      return "";
    }
  }

  private String hash(String value) {
    try {
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          value.getBytes(StandardCharsets.UTF_8)
        )
      );
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private String encode(String value) {
    return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8).replace(
      "+",
      "%20"
    );
  }
}
