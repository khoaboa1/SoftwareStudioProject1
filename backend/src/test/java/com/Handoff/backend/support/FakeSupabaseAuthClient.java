package com.Handoff.backend.support;

import com.Handoff.backend.service.InvalidCredentialsException;
import com.Handoff.backend.service.InvalidVerificationPinException;
import com.Handoff.backend.service.auth.EmailNotConfirmedException;
import com.Handoff.backend.service.auth.SupabaseAuthClient;
import com.Handoff.backend.service.auth.SupabaseSession;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory Supabase Auth used by every Spring test context, so tests never
 * call the network.
 */
@Primary
@Component
public class FakeSupabaseAuthClient implements SupabaseAuthClient {
  public static final String SIGNUP_OTP = "123456";
  public static final String ACCESS_PREFIX = "fake-access:";
  public static final String REFRESH_PREFIX = "fake-refresh:";

  private record FakeUser(UUID id, String password, boolean confirmed) {
  }

  private final Map<String, FakeUser> users = new ConcurrentHashMap<>();
  private final List<String> resentSignupOtps = new CopyOnWriteArrayList<>();

  public static String accessTokenFor(UUID userId) {
    return ACCESS_PREFIX + userId;
  }

  public static String refreshTokenFor(UUID userId) {
    return REFRESH_PREFIX + userId;
  }

  public void reset() {
    users.clear();
    resentSignupOtps.clear();
  }

  public List<String> resentSignupOtps() {
    return List.copyOf(resentSignupOtps);
  }

  public UUID userIdFor(String email) {
    FakeUser user = users.get(email);
    return user == null ? null : user.id();
  }

  @Override
  public boolean isConfigured() {
    return false;
  }

  @Override
  public UUID signUp(String email, String password) {
    FakeUser existing = users.get(email);
    UUID id = existing == null ? UUID.randomUUID() : existing.id();
    users.put(email, new FakeUser(id, password, false));
    return id;
  }

  @Override
  public SupabaseSession passwordGrant(String email, String password) {
    FakeUser user = users.get(email);
    if (user == null || !user.password().equals(password)) {
      throw new InvalidCredentialsException();
    }
    if (!user.confirmed()) {
      throw new EmailNotConfirmedException();
    }
    return sessionFor(user.id());
  }

  @Override
  public SupabaseSession refreshGrant(String refreshToken) {
    if (refreshToken == null || !refreshToken.startsWith(REFRESH_PREFIX)) {
      throw new InvalidCredentialsException("Please log in again.");
    }
    return sessionFor(UUID.fromString(refreshToken.substring(REFRESH_PREFIX.length())));
  }

  @Override
  public SupabaseSession verifySignupOtp(String email, String token) {
    FakeUser user = users.get(email);
    if (user == null || !SIGNUP_OTP.equals(token)) {
      throw new InvalidVerificationPinException("Incorrect or expired verification PIN");
    }
    users.put(email, new FakeUser(user.id(), user.password(), true));
    return sessionFor(user.id());
  }

  @Override
  public void resendSignupOtp(String email) {
    resentSignupOtps.add(email);
  }

  @Override
  public void logout(String accessToken) {
    // Nothing to revoke in memory; the backend clears the cookies.
  }

  @Override
  public UUID adminCreateConfirmedUser(String email, String password) {
    UUID id = UUID.randomUUID();
    users.put(email, new FakeUser(id, password, true));
    return id;
  }

  private static SupabaseSession sessionFor(UUID id) {
    return new SupabaseSession(accessTokenFor(id), refreshTokenFor(id), 3600, id);
  }
}
