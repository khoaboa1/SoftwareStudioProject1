package com.Handoff.backend.service.auth;

import com.Handoff.backend.service.EmailAlreadyRegisteredException;
import com.Handoff.backend.service.InvalidCredentialsException;
import com.Handoff.backend.service.InvalidSignupException;
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
    void passwordGrant_returnsSession() {
        server.expect(requestTo(BASE + "/auth/v1/token?grant_type=password")).andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "anon-key")).andExpect(jsonPath("$.email").value("jane@tulane.edu"))
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
                .isExactlyInstanceOf(InvalidCredentialsException.class).hasMessage("Invalid email or password");
    }

    @Test
    void passwordGrant_keepsRateLimitStatus() {
        expectError("/auth/v1/token?grant_type=password", HttpStatus.TOO_MANY_REQUESTS,
                "{\"code\":429,\"error_code\":\"over_request_rate_limit\",\"msg\":\"Too many requests\"}");
        assertThatThrownBy(() -> client.passwordGrant("jane@tulane.edu", "Password123!"))
                .isExactlyInstanceOf(SupabaseAuthException.class).hasFieldOrPropertyWithValue("status", 429);
    }

    @Test
    void refreshGrant_mapsRejectedTokenToLogInAgain() {
        expectError("/auth/v1/token?grant_type=refresh_token", HttpStatus.BAD_REQUEST,
                "{\"code\":400,\"error_code\":\"refresh_token_not_found\",\"msg\":\"Invalid Refresh Token\"}");
        assertThatThrownBy(() -> client.refreshGrant("old-refresh-token"))
                .isExactlyInstanceOf(InvalidCredentialsException.class).hasMessage("Please log in again.");
    }

    @Test
    void logout_sendsAccessTokenAsBearer() {
        server.expect(requestTo(BASE + "/auth/v1/logout")).andExpect(header("apikey", "anon-key"))
                .andExpect(header("Authorization", "Bearer at")).andRespond(withNoContent());
        client.logout("at");
        server.verify();
    }

    @Test
    void adminCreateUnconfirmedUser_postsUnconfirmedUserWithServiceKey() {
        server.expect(requestTo(BASE + "/auth/v1/admin/users")).andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", "service-key")).andExpect(header("Authorization", "Bearer service-key"))
                .andExpect(jsonPath("$.email").value("jane@tulane.edu"))
                .andExpect(jsonPath("$.password").value("Password123!"))
                .andExpect(jsonPath("$.email_confirm").value(false))
                .andRespond(withSuccess("{\"id\":\"" + USER_ID + "\"}", MediaType.APPLICATION_JSON));
        assertThat(client.adminCreateUnconfirmedUser("jane@tulane.edu", "Password123!"))
                .isEqualTo(UUID.fromString(USER_ID));
        server.verify();
    }

    @Test
    void adminCreateUnconfirmedUser_mapsExistingEmailToConflict() {
        expectError("/auth/v1/admin/users", HttpStatus.UNPROCESSABLE_ENTITY,
                "{\"code\":422,\"error_code\":\"email_exists\",\"msg\":\"A user with this email already exists\"}");
        assertThatThrownBy(() -> client.adminCreateUnconfirmedUser("jane@tulane.edu", "Password123!"))
                .isExactlyInstanceOf(EmailAlreadyRegisteredException.class).hasMessage("Email already registered");
    }

    @Test
    void adminCreateUnconfirmedUser_mapsWeakPasswordToInvalidSignup() {
        expectError("/auth/v1/admin/users", HttpStatus.UNPROCESSABLE_ENTITY,
                "{\"code\":422,\"error_code\":\"weak_password\",\"msg\":\"Password is too weak\"}");
        assertThatThrownBy(() -> client.adminCreateUnconfirmedUser("jane@tulane.edu", "12345678"))
                .isExactlyInstanceOf(InvalidSignupException.class).hasMessage("Password is too weak");
    }

    @Test
    void adminUpdateUser_putsPasswordAndConfirmation() {
        server.expect(requestTo(BASE + "/auth/v1/admin/users/" + USER_ID)).andExpect(method(HttpMethod.PUT))
                .andExpect(header("apikey", "service-key")).andExpect(header("Authorization", "Bearer service-key"))
                .andExpect(jsonPath("$.password").value("NewPassword1!"))
                .andExpect(jsonPath("$.email_confirm").value(false))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        client.adminUpdateUser(UUID.fromString(USER_ID), "NewPassword1!", false);
        server.verify();
    }

    @Test
    void adminUpdateUser_omitsFieldsThatAreNull() {
        server.expect(requestTo(BASE + "/auth/v1/admin/users/" + USER_ID)).andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.email_confirm").value(true)).andExpect(jsonPath("$.password").doesNotExist())
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        client.adminUpdateUser(UUID.fromString(USER_ID), null, true);
        server.verify();
    }

    @Test
    void adminFindUserByEmail_matchesIgnoringCaseAndReadsConfirmation() {
        server.expect(requestTo(BASE + "/auth/v1/admin/users?page=1&per_page=200")).andExpect(method(HttpMethod.GET))
                .andExpect(header("apikey", "service-key"))
                .andRespond(withSuccess("{\"users\":[{\"id\":\"22222222-2222-2222-2222-222222222222\","
                        + "\"email\":\"other@tulane.edu\",\"email_confirmed_at\":null},{\"id\":\"" + USER_ID + "\","
                        + "\"email\":\"Jane@Tulane.edu\",\"email_confirmed_at\":\"2026-11-14T10:00:00Z\"}]}",
                        MediaType.APPLICATION_JSON));
        assertThat(client.adminFindUserByEmail("jane@tulane.edu"))
                .contains(new SupabaseAdminUser(UUID.fromString(USER_ID), true));
        server.verify();
    }

    @Test
    void adminFindUserByEmail_returnsEmptyWhenNoUserMatches() {
        server.expect(requestTo(BASE + "/auth/v1/admin/users?page=1&per_page=200"))
                .andRespond(withSuccess(
                        "{\"users\":[{\"id\":\"" + USER_ID
                                + "\",\"email\":\"other@tulane.edu\",\"email_confirmed_at\":null}]}",
                        MediaType.APPLICATION_JSON));
        assertThat(client.adminFindUserByEmail("jane@tulane.edu")).isEmpty();
        server.verify();
    }

    @Test
    void adminCreateConfirmedUser_usesServiceKey() {
        server.expect(requestTo(BASE + "/auth/v1/admin/users")).andExpect(header("apikey", "service-key"))
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
                .isExactlyInstanceOf(SupabaseAuthException.class).hasFieldOrPropertyWithValue("status", 503);
    }
}
