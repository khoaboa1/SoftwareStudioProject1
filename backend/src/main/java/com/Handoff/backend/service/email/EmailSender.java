package com.Handoff.backend.service.email;

/**
 * One way of delivering a verification PIN email. VerificationService tries each
 * registered sender in {@code @Order} until one succeeds, so adding a provider
 * means adding a new implementation rather than editing the service.
 */
public interface EmailSender {

  /**
   * @return true if the email was delivered; false if this sender is not
   *         configured or the delivery failed (so the next sender should be tried)
   */
  boolean send(String to, String pin);
}
