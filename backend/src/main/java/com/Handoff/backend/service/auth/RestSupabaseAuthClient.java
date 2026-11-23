package com.Handoff.backend.service.auth;

import com.Handoff.backend.service.EmailAlreadyRegisteredException;
import com.Handoff.backend.service.InvalidCredentialsException;
import com.Handoff.backend.service.InvalidSignupException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.json.JsonMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Calls the Supabase Auth REST API (GoTrue) on behalf of the frontend. */
@Component
public class RestSupabaseAuthClient implements SupabaseAuthClient {
    private final RestClient rest;
    private final String baseUrl;
    private final String anonKey;
    private final String serviceKey;
    private final JsonMapper json = JsonMapper.builder().build();
    private static final int USERS_PER_PAGE = 200;
    private static final int MAX_USER_PAGES = 50;

    @Autowired
    public RestSupabaseAuthClient(@Value("${supabase.url:}") String baseUrl,
            @Value("${supabase.anon-key:}") String anonKey, @Value("${supabase.service-key:}") String serviceKey) {
        this(RestClient.builder(), baseUrl, anonKey, serviceKey);
    }

    RestSupabaseAuthClient(RestClient.Builder builder, String baseUrl, String anonKey, String serviceKey) {
        this.rest = builder.build();
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.anonKey = anonKey == null ? "" : anonKey;
        this.serviceKey = serviceKey == null ? "" : serviceKey;
    }

    @Override
    public boolean isConfigured() {
        return !baseUrl.isBlank() && !anonKey.isBlank();
    }

    @Override
    public SupabaseSession passwordGrant(String email, String password) {
        try {
            return session(call("/auth/v1/token?grant_type=password", anonKey, null,
                    Map.of("email", email, "password", password)));
        } catch (SupabaseAuthException ex) {
            if ("email_not_confirmed".equals(ex.getCode())) {
                throw new EmailNotConfirmedException();
            }
            if ("invalid_credentials".equals(ex.getCode()) || ex.getStatus() == 400) {
                throw new InvalidCredentialsException();
            }
            throw ex;
        }
    }

    @Override
    public SupabaseSession refreshGrant(String refreshToken) {
        try {
            return session(call("/auth/v1/token?grant_type=refresh_token", anonKey, null,
                    Map.of("refresh_token", refreshToken)));
        } catch (SupabaseAuthException ex) {
            if (isClientError(ex)) {
                throw new InvalidCredentialsException("Please log in again.");
            }
            throw ex;
        }
    }

    @Override
    public void logout(String accessToken) {
        call("/auth/v1/logout", anonKey, accessToken, Map.of());
    }

    @Override
    public UUID adminCreateConfirmedUser(String email, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("email_confirm", true);
        return userId(call("/auth/v1/admin/users", serviceKey, serviceKey, body));
    }

    @Override
    public UUID adminCreateUnconfirmedUser(String email, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        body.put("email_confirm", false);
        try {
            return userId(call("/auth/v1/admin/users", serviceKey, serviceKey, body));
        } catch (SupabaseAuthException ex) {
            if ("email_exists".equals(ex.getCode()) || "user_already_exists".equals(ex.getCode())) {
                throw new EmailAlreadyRegisteredException("Email already registered");
            }
            if ("weak_password".equals(ex.getCode())) {
                throw new InvalidSignupException(ex.getMessage());
            }
            throw ex;
        }
    }

    @Override
    public void adminUpdateUser(UUID userId, String password, Boolean emailConfirmed) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (password != null) {
            body.put("password", password);
        }
        if (emailConfirmed != null) {
            body.put("email_confirm", emailConfirmed);
        }
        callWithMethod(HttpMethod.PUT, "/auth/v1/admin/users/" + userId, serviceKey, serviceKey, body);
    }

    @Override
    public Optional<SupabaseAdminUser> adminFindUserByEmail(String email) {
        for (int page = 1; page <= MAX_USER_PAGES; page++) {
            Map<String, Object> body = callWithMethod(HttpMethod.GET,
                    "/auth/v1/admin/users?page=" + page + "&per_page=" + USERS_PER_PAGE, serviceKey, serviceKey, null);
            if (!(body.get("users") instanceof List<?> users) || users.isEmpty()) {
                return Optional.empty();
            }
            for (Object item : users) {
                if (item instanceof Map<?, ?> user && email.equalsIgnoreCase(text(user.get("email")))) {
                    return Optional.of(new SupabaseAdminUser(UUID.fromString(text(user.get("id"))),
                            !text(user.get("email_confirmed_at")).isEmpty()));
                }
            }
            if (users.size() < USERS_PER_PAGE) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private static boolean isClientError(SupabaseAuthException ex) {
        return ex.getStatus() >= 400 && ex.getStatus() < 500 && ex.getStatus() != 429;
    }

    private Map<String, Object> call(String path, String apiKey, String bearer, Map<String, ?> body) {
        return callWithMethod(HttpMethod.POST, path, apiKey, bearer, body);
    }

    private Map<String, Object> callWithMethod(HttpMethod method, String path, String apiKey, String bearer,
            Map<String, ?> body) {
        if (baseUrl.isBlank() || apiKey.isBlank()) {
            throw new SupabaseAuthException(503, "not_configured", "Supabase Auth is not configured");
        }
        try {
            RestClient.RequestBodySpec request = rest.method(method).uri(baseUrl + path).header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON);
            if (bearer != null && !bearer.isBlank()) {
                request = request.header("Authorization", "Bearer " + bearer);
            }
            if (body != null) {
                request = request.body(json.writeValueAsString(body));
            }
            return parse(request.retrieve().body(String.class));
        } catch (RestClientResponseException ex) {
            Map<String, Object> error = parse(ex.getResponseBodyAsString());
            String code = text(error.get("error_code"));
            if (code.isEmpty()) {
                code = text(error.get("error"));
            }
            String message = firstNonBlank(text(error.get("msg")), text(error.get("error_description")),
                    text(error.get("message")), "Supabase Auth request failed");
            throw new SupabaseAuthException(ex.getStatusCode().value(), code, message);
        } catch (RestClientException ex) {
            throw new SupabaseAuthException(502, "unreachable", "Supabase Auth is unreachable");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return json.readValue(body, Map.class);
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private SupabaseSession session(Map<String, Object> body) {
        String accessToken = text(body.get("access_token"));
        if (accessToken.isEmpty()) {
            throw new SupabaseAuthException(502, "bad_response", "Supabase Auth returned no session");
        }
        long expiresIn = body.get("expires_in") instanceof Number number ? number.longValue() : 3600L;
        return new SupabaseSession(accessToken, text(body.get("refresh_token")), expiresIn, userId(body));
    }

    private static UUID userId(Map<String, Object> body) {
        Object id = body.get("id");
        if (id == null && body.get("user") instanceof Map<?, ?> user) {
            id = user.get("id");
        }
        if (id == null) {
            throw new SupabaseAuthException(502, "bad_response", "Supabase Auth returned no user id");
        }
        return UUID.fromString(id.toString());
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
