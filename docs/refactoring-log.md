# Refactoring Log

## SSP1-98 — Extract shared university email validation policy

- **Date:** 2026-10-26
- **Code smell:** `AuthService` and `VerificationService` each owned the same university email regex. Four validation checks repeated its null handling and matching logic.
- **Mechanic:** Extract Class/Policy. Move the regex and null check into a stateless Spring `UniversityEmailValidator` component, constructor-inject it into both services, and delegate all four checks to `isValid(String email)`.
- **Implementation commit:** [5cf1836 — extract shared university email validation policy](https://github.com/khoaboa1/SoftwareStudioProject1/commit/5cf183670b55e98124f9b26b9a721c81ab78440c)

### Before

Both services declared this policy independently:

```java
private static final Pattern EDU_EMAIL_PATTERN =
    Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);
```

For example, signup checked the pattern directly:

```java
if (email == null || !EDU_EMAIL_PATTERN.matcher(email).matches()) {
  throw new InvalidSignupException("Use a valid .edu school email");
}
```

### After

One component owns the rule:

```java
@Component
public class UniversityEmailValidator {
  private static final Pattern EDU_EMAIL_PATTERN =
      Pattern.compile("^[^\\s@]+@[^\\s@]+\\.edu$", Pattern.CASE_INSENSITIVE);

  public boolean isValid(String email) {
    return email != null && EDU_EMAIL_PATTERN.matcher(email).matches();
  }
}
```

Services delegate validation while retaining their existing exceptions and messages:

```java
if (!universityEmailValidator.isValid(email)) {
  throw new InvalidSignupException("Use a valid .edu school email");
}
```

### Compatibility

- Signup, PIN verification, PIN resend, and direct PIN generation use the shared policy. Each retains its original exception type, message, and validation order.
- The exact case-insensitive regex is preserved. Input is neither trimmed nor normalized; uppercase domains, subdomains, plus addressing, and previously accepted permissive forms retain their behavior.
- Repository lookups, PIN expiry, email-sender fallback, and sessions retain their existing behavior. HTTP endpoints, response shapes/statuses, and the database schema are unchanged; no frontend integration changes are needed.
- All existing test files and test cases are retained. Only the four constructor calls in `VerificationServicePinDispatchTest` were updated to pass the validator as the third argument.

### Verification

Executed under Temurin JDK 21.0.13:

- **New coverage:** 170 test invocations across `UniversityEmailValidatorTest`, `AuthServiceEmailValidationTest`, `VerificationServiceEmailValidationTest`, and `AuthEmailValidationControllerTest`. Shared cases cover accepted and rejected forms through the policy and every flow. Tests also check exact errors, validation order, rejection before repository/sender interactions, PIN state, success responses, and session creation.
- **Focused run:** 187 tests passed, including the existing authentication, verification, and sender-dispatch tests.
- **Full backend suite:** 315 tests across 24 classes passed; zero failures, errors, or skips.
- **Whitespace check:** `git diff --check` passed.

From `backend/`, with `JAVA_HOME` pointing to JDK 21:

```bash
./mvnw -q -Dtest=UniversityEmailValidatorTest,AuthServiceEmailValidationTest,VerificationServiceEmailValidationTest,AuthEmailValidationControllerTest,VerificationServicePinDispatchTest,AuthControllerTest,VerificationServiceTest test
./mvnw -q test
```
