package com.portfolio_hub.userauthmgt.user;

import com.portfolio_hub.utils.exception.InvalidInputException;

public final class WhatsAppNumber {

  private WhatsAppNumber() {}

  public static String normalize(String value) {
    if (value == null || value.isBlank()) {
      throw new InvalidInputException(
        "Enter the WhatsApp number you actively use, including the country code"
      );
    }

    String normalized = value.trim().replaceAll("[\\s().-]", "");
    if (normalized.startsWith("00")) {
      normalized = "+" + normalized.substring(2);
    }
    if (!normalized.matches("^\\+[1-9][0-9]{7,14}$")) {
      throw new InvalidInputException(
        "Enter a valid WhatsApp number with the country code, for example +2348012345678"
      );
    }
    return normalized;
  }
}
