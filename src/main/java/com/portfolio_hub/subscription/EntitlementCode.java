package com.portfolio_hub.subscription;

import java.util.Locale;

public enum EntitlementCode {
  PAGES(EntitlementValueType.INTEGER),
  SECTIONS(EntitlementValueType.INTEGER),
  PRODUCTS(EntitlementValueType.INTEGER),
  MUSIC_TRACKS(EntitlementValueType.INTEGER),
  TEAM_MEMBERS(EntitlementValueType.INTEGER),
  STORAGE_MB(EntitlementValueType.INTEGER),
  EMAIL_TEMPLATES(EntitlementValueType.INTEGER),
  MONTHLY_EMAILS(EntitlementValueType.INTEGER),
  VIDEO_BACKGROUNDS(EntitlementValueType.BOOLEAN),
  CUSTOM_DOMAIN(EntitlementValueType.BOOLEAN),
  ADVANCED_ANIMATIONS(EntitlementValueType.BOOLEAN),
  REMOVE_BRANDING(EntitlementValueType.BOOLEAN),
  CART_ORDERS(EntitlementValueType.BOOLEAN),
  WHATSAPP_ORDERS(EntitlementValueType.BOOLEAN);

  private final EntitlementValueType valueType;

  EntitlementCode(EntitlementValueType valueType) {
    this.valueType = valueType;
  }

  public EntitlementValueType valueType() {
    return valueType;
  }

  public String displayName() {
    String value = name().toLowerCase(Locale.ROOT).replace('_', ' ');
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}
