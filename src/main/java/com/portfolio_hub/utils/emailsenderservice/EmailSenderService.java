package com.portfolio_hub.utils.emailsenderservice;

public interface EmailSenderService {
  void sendVerificationEmail(
    String recipient,
    String fullName,
    String verificationUrl
  );
  void sendPasswordResetEmail(
    String recipient,
    String fullName,
    String resetUrl
  );
  void sendNewEnquiryNotification(
    String recipient,
    String fullName,
    String recruiterName,
    String recruiterEmail,
    String company,
    String message,
    String dashboardUrl
  );
  void sendEmail(
    String senderEmail,
    String recipientEmail,
    String subject,
    String body
  );
}
