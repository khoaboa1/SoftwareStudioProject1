package com.Handoff.backend.service.auth;

import com.Handoff.backend.service.EmailAlreadyRegisteredException;
import com.Handoff.backend.service.InvalidCredentialsException;
import com.Handoff.backend.service.InvalidVerificationPinException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestSupabaseAuthClientTest {

  private static final String BASE = "https://project.supabase.co";
  private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
  private static final String SESSION_JSON = "{\"access_token\":\"at\",\"refresh_token\":\"rt\",\"expires_in\":3600,"
      + "\"user\":{\"id\":\"" + USER_ID + "\"}}";
  private MockRestServiceServer server;
  private RestSupabaseAuthClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new RestSupabaseAuthClient(builder, BASE + "/", "anon-key", "service-key");
  }

  private void expectError(String path, HttpStatus status, String body) {
    server.expect(requestTo(BASE + path))
        .andRespond(withStatus(status).contentType(MediaType.APPLICATION_JSON).body(body));
  }

  @Test
  void signUp_postsCredentialsWithAnonKey_andReturnsUserId() {
    server.expect(requestTo(BASE + "/auth/v1/signup"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("apikey", "anon-key"))
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
        .andExpect(jsonPath("$.password").value("Password123!"))
        .andRespond(withSuccess("{\"id\":\"" + USER_ID + "\",\"email\":\"jane@tulane.edu\"}",
            MediaType.APPLICATION_JSON));
    assertThat(client.signUp("jane@tulane.edu", "Password123!")).isEqualTo(UUID.fromString(USER_ID));
    server.verify();
  }

  @Test
  void signUp_mapsExistingUserToConflict() {
    expectError("/auth/v1/signup", HttpStatus.UNPROCESSABLE_ENTITY,
        "{\"code\":422,\"error_code\":\"user_already_exists\",\"msg\":\"User already registered\"}");
    assertThatThrownBy(() -> client.signUp("jane@tulane.edu", "Password123!"))
        .isExactlyInstanceOf(EmailAlreadyRegisteredException.class)
        .hasMessage("Email already registered");
  }

  @Test
  void passwordGrant_returnsSession() {
    server.expect(requestTo(BASE + "/auth/v1/token?grant_type=password"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("apikey", "anon-key"))
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
        .andExpect(jsonPath("$.password").value("Password123!"))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));
    SupabaseSession session = client.passwordGrant("jane@tulane.edu", "Password123!");
    assertThat(session).isEqualTo(new SupabaseSession("at", "rt", 3600, UUID.fromString(USER_ID)));
    server.verify();
  }

  @Test
  void passwordGrant_mapsUnconfirmedEmail() {
    expectError("/auth/v1/token?grant_type=password", HttpStatus.BAD_REQUEST,
        "{\"code\":400,\"error_code\":\"email_not_confirmed\",\"msg\":\"Email not confirmed\"}");
    assertThatThrownBy(() -> client.passwordGrant("jane@tulane.edu", "Password123!"))
        .isExactlyInstanceOf(EmailNotConfirmedException.class);
  }

  @Test
  void passwordGrant_mapsBadCredentials() {
    expectError("/auth/v1/token?grant_type=password", HttpStatus.BAD_REQUEST,
        "{\"code\":400,\"error_code\":\"invalid_credentials\",\"msg\":\"Invalid login credentials\"}");
    assertThatThrownBy(() -> client.passwordGrant("jane@tulane.edu", "wrong-password"))
        .isExactlyInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Invalid email or password");
  }

  @Test
  void passwordGrant_keepsRateLimitStatus() {
    expectError("/auth/v1/token?grant_type=password", HttpStatus.TOO_MANY_REQUESTS,
        "{\"code\":429,\"error_code\":\"over_request_rate_limit\",\"msg\":\"Too many requests\"}");
    assertThatThrownBy(() -> client.passwordGrant("jane@tulane.edu", "Password123!"))
        .isExactlyInstanceOf(SupabaseAuthException.class)
        .hasFieldOrPropertyWithValue("status", 429);
  }

  @Test
  void verifySignupOtp_postsEmailCode_andReturnsSession() {
    server.expect(requestTo(BASE + "/auth/v1/verify"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.type").value("email"))
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
        .andExpect(jsonPath("$.token").value("123456"))
        .andRespond(withSuccess(SESSION_JSON, MediaType.APPLICATION_JSON));
    assertThat(client.verifySignupOtp("jane@tulane.edu", "123456").accessToken()).isEqualTo("at");
    server.verify();
  }

  @Test
  void verifySignupOtp_mapsExpiredCode() {
    expectError("/auth/v1/verify", HttpStatus.FORBIDDEN,
        "{\"code\":403,\"error_code\":\"otp_expired\",\"msg\":\"Token has expired or is invalid\"}");
    assertThatThrownBy(() -> client.verifySignupOtp("jane@tulane.edu", "000000"))
        .isExactlyInstanceOf(InvalidVerificationPinException.class)
        .hasMessage("Incorrect or expired verification PIN");
  }

  @Test
  void refreshGrant_mapsRejectedTokenToLogInAgain() {
    expectError("/auth/v1/token?grant_type=refresh_token", HttpStatus.BAD_REQUEST,
        "{\"code\":400,\"error_code\":\"refresh_token_not_found\",\"msg\":\"Invalid Refresh Token\"}");
    assertThatThrownBy(() -> client.refreshGrant("old-refresh-token"))
        .isExactlyInstanceOf(InvalidCredentialsException.class)
        .hasMessage("Please log in again.");
  }

  @Test
  void resendSignupOtp_postsSignupType() {
    server.expect(requestTo(BASE + "/auth/v1/resend"))
        .andExpect(jsonPath("$.type").value("signup"))
        .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    client.resendSignupOtp("jane@tulane.edu");
    server.verify();
  }

  @Test
  void logout_sendsAccessTokenAsBearer() {
    server.expect(requestTo(BASE + "/auth/v1/logout"))
        .andExpect(header("apikey", "anon-key"))
        .andExpect(header("Authorization", "Bearer at"))
        .andRespond(withNoContent());
    client.logout("at");
    server.verify();
  }

  @Test
  void adminCreateConfirmedUser_usesServiceKey() {
    server.expect(requestTo(BASE + "/auth/v1/admin/users"))
        .andExpect(header("apikey", "service-key"))
        .andExpect(header("Authorization", "Bearer service-key"))
        .andExpect(jsonPath("$.email").value("sarah@tulane.edu"))
        .andExpect(jsonPath("$.email_confirm").value(true))
        .andRespond(withSuccess("{\"id\":\"" + USER_ID + "\"}", MediaType.APPLICATION_JSON));
    assertThat(client.adminCreateConfirmedUser("sarah@tulane.edu", "Password123!"))
        .isEqualTo(UUID.fromString(USER_ID));
    server.verify();
  }

  @Test
  void unconfiguredClient_failsWithoutCallingTheNetwork() {
    RestSupabaseAuthClient unconfigured = new RestSupabaseAuthClient(RestClient.builder(), "", "", "");
    assertThat(unconfigured.isConfigured()).isFalse();
    assertThatThrownBy(() -> unconfigured.passwordGrant("jane@tulane.edu", "Password123!"))
        .isExactlyInstanceOf(SupabaseAuthException.class)
        .hasFieldOrPropertyWithValue("status", 503);
  }
}
