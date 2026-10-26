package com.Handoff.backend.service.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** SMTP via JavaMailSender — last-resort fallback after the HTTPS senders. */
@Component
@Order(3)
public class SmtpEmailSender implements EmailSender {

  private static final Logger logger = LoggerFactory.getLogger(SmtpEmailSender.class);

  private final JavaMailSender mailSender;
  private final String senderEmail;

  public SmtpEmailSender(@Autowired(required = false) JavaMailSender mailSender,
                         @Value("${spring.mail.username:}") String senderEmail) {
    this.mailSender = mailSender;
    this.senderEmail = senderEmail;
  }

  @Override
  public boolean send(String to, String pin) {
    if (mailSender == null) {
      return false;
    }
    try {
      SimpleMailMessage message = new SimpleMailMessage();
      if (senderEmail != null && !senderEmail.isBlank()) {
        message.setFrom(senderEmail);
      }
      message.setTo(to);
      message.setSubject("Your Handoff Verification PIN: " + pin);
      message.setText("Welcome to Handoff!\n\n" +
          "Your 6-digit verification PIN is: " + pin + "\n\n" +
          "This PIN will expire in 15 minutes.\n\n" +
          "Best,\n" +
          "The Handoff Team");

      mailSender.send(message);
      logger.info("[EMAIL DISPATCH] Successfully sent email to {}", to);
      return true;
    } catch (Exception ex) {
      logger.warn("[EMAIL DISPATCH WARNING] Could not send email via SMTP: {}. " +
          "PIN is still available in backend console.", ex.getMessage());
      return false;
    }
  }
}
