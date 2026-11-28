# Software Studio Project 1

## Setup

```bash
git clone https://github.com/khoaboa1/SoftwareStudioProject1.git
cd SoftwareStudioProject1
./scripts/setup-dev.sh      # or: bash scripts/setup-dev.sh
```

`setup-dev.sh` points git at the repo's hooks and installs the commit-message template.

### Database (Supabase)

The backend uses a hosted Supabase PostgreSQL database. Credentials are never committed.

1. Create `backend/.env` (gitignored).
2. In the Supabase dashboard, open **Connect → Session pooler** and fill in `DB_URL`
   (JDBC form, `?sslmode=require`), `DB_USERNAME` (`postgres.<project-ref>`) and `DB_PASSWORD`.
3. Run the backend: `cd backend && ./mvnw spring-boot:run`. Flyway creates/updates the schema on startup.

To load demo students and listings in development, run `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.

### Listing photo storage

Create a **public** Supabase Storage bucket named `listing-images`. Set `SUPABASE_URL` and
`SUPABASE_SERVICE_KEY` in the gitignored `backend/.env` to enable authenticated photo uploads.
The service key is used only by the backend. `SUPABASE_STORAGE_BUCKET` can override the bucket name.
Without these settings, listing creation still works, but photo uploads return 503.

Environment variables with the same names override `.env`.

## Design patterns

Four patterns from the codebase. Each entry says where the pattern is, why we chose it, and shows the code.

### 1. Strategy (with a fallback chain): sending the verification PIN email

**Where:**
- `backend/src/main/java/com/Handoff/backend/service/email/EmailSender.java` is the strategy interface.
- `WebhookEmailSender`, `ResendEmailSender` and `SmtpEmailSender`, in the same package, are the concrete strategies, ordered with `@Order(1)`, `@Order(2)` and `@Order(3)`.
- `backend/src/main/java/com/Handoff/backend/service/VerificationService.java` is the context: Spring injects every `EmailSender` as a `List`, sorted by `@Order`.

**Why:**
- Campus Wi-Fi blocks SMTP ports, so delivery has to fall back to HTTPS providers.
- Each provider is configured separately and may be missing in a given environment.
- With Strategy, `VerificationService` never needs to know which provider exists. Adding a provider means adding a class, not editing the service (open/closed).
- Each sender returns `false` when it isn't configured or fails, so the service tries the next one. That makes the list a simple fallback chain.

```java
// The strategy
public interface EmailSender {
    boolean send(String to, String pin);   // false = not configured or failed, try the next one
}

// One concrete strategy
@Component
@Order(1)
public class WebhookEmailSender implements EmailSender { ... }

// The context: tries each strategy in @Order until one succeeds
for (EmailSender sender : emailSenders) {
    if (sender.send(student.getEmail(), pin)) {
        break;
    }
}
```

**Before/after:** without the pattern, `VerificationService` would need an `if (webhookUrl set) … else if (resend key set) … else smtp` block that changes with every provider. Now its loop never changes, and tests pass a `List.of(...)` of mock senders (`VerificationServicePinDispatchTest`).

### 2. Adapter: Supabase Auth behind `SupabaseAuthClient`

**Where:**
- `backend/src/main/java/com/Handoff/backend/service/auth/SupabaseAuthClient.java` is the target interface that our code depends on.
- `backend/src/main/java/com/Handoff/backend/service/auth/RestSupabaseAuthClient.java` is the adapter. It turns calls such as `passwordGrant(email, password)` into Supabase's REST requests and parses the JSON back into `SupabaseSession` and `SupabaseAdminUser` records.
- `backend/src/test/java/com/Handoff/backend/support/FakeSupabaseAuthClient.java` is an in-memory implementation. It is `@Primary` in tests, so no test calls the real Supabase.

**Why:**
- Supabase's API speaks HTTP and JSON with its own field names. `AuthService` should work with our own types and stay unaware of URLs, keys and JSON.
- Because of the adapter, the whole auth flow (sign-up, PIN, login, refresh) is tested end to end with `FakeSupabaseAuthClient`.
- Changing the auth provider later would only mean writing a new adapter.

```java
/** The only way the backend talks to Supabase Auth. The frontend never calls Supabase directly. */
public interface SupabaseAuthClient {
    SupabaseSession passwordGrant(String email, String password);
    SupabaseSession refreshGrant(String refreshToken);
    UUID adminCreateUnconfirmedUser(String email, String password);
    Optional<SupabaseAdminUser> adminFindUserByEmail(String email);
    // ...
}

@Component
public class RestSupabaseAuthClient implements SupabaseAuthClient { ... }   // HTTP + JSON

@Primary
@Component
public class FakeSupabaseAuthClient implements SupabaseAuthClient { ... }   // tests only
```

### 3. Facade: `createAuthFacade` on the frontend

**Where:**
- `frontend/src/services/authFacade.ts` is the facade.
- `frontend/src/pages/LoginPage.tsx` and `frontend/src/components/AppHeader.tsx` use it.

**Why:** signing in touches three subsystems:
- the HTTP calls in `authService.ts` (`login`, `verifyPin`, `logout`);
- the per-browser device id (`lib/device.ts`);
- the auth state in React (`setStudent`).

The facade gives pages one call per user action, so the pages don't repeat the coordination and can't forget a step, such as sending the device id or clearing the student on logout.

```ts
export function createAuthFacade(setStudent: (student: Student | null) => void) {
  return {
    async signIn(input: { email: string; password: string }) {
      const result = await login({ ...input, deviceId: getOrCreateDeviceId() })
      const student = studentFromLogin(result)
      if (student) setStudent(student)
      return result
    },
    async signOut() {
      await logout()
      setStudent(null)
    },
    // verifySignIn(...)
  }
}

// In a page: one call instead of three subsystems
const auth = createAuthFacade(setStudent)
await auth.signIn({ email, password })
```

### 4. Repository: Spring Data repositories

**Where:** `backend/src/main/java/com/Handoff/backend/repository/`, for example:
- `ListingRepository.java`: named JPQL queries plus `JpaSpecificationExecutor` for the optional browse filters in `ListingSpecifications.java`;
- `ProfileRepository.java`;
- `StudentRepository.java`.

**Why:**
- Services ask for domain objects by meaning, for example `findBySchoolDomain(schoolDomain)`, and never write SQL or touch the `EntityManager`.
- Each query, including its school-scoping rule, lives in one place.
- The repositories are interfaces, so service tests can mock them (`VerificationServiceHardeningTest`), while repository tests check the real queries on H2 (`ProfileSearchQueryTest`).

```java
public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    @Query("""
            select listing from Listing listing
            join fetch listing.seller seller
            where seller.suspended = false and exists (
              select profile.id from Profile profile
              where profile.student = seller and profile.schoolDomain = :schoolDomain
            )
            order by listing.createdAt desc
            """)
    List<Listing> findBySchoolDomain(@Param("schoolDomain") String schoolDomain);
}
```

For the refactorings that introduced or tightened some of these structures (Extract Component, Extract Class, Extract Hook), see [`docs/refactoring-log.md`](docs/refactoring-log.md).

## Jira workflow

Every branch and commit is tied to a Jira issue so work shows up automatically on the
Jira board (Development panel, commits, branches, PRs).

**Branch naming** — `<type>/<ISSUE-KEY>-<short-description>`

```
feature/SSP1-12-user-login
bugfix/SSP1-27-fix-null-avatar
chore/SSP1-3-ci-pipeline
```

**Commit messages** — start the subject with the issue key:

```
SSP1-12 Add password hashing to signup flow
```

The `prepare-commit-msg` hook pulls the key out of your branch name automatically,
so most of the time you just type the message. The `commit-msg` hook rejects commits
with no key.

**Smart Commits** — Jira acts on these keywords in a commit message:

| Syntax | Effect |
|---|---|
| `SSP1-12 #comment Fixed the race condition` | Adds a comment to the issue |
| `SSP1-12 #time 2h 30m Refactoring` | Logs work against the issue |
| `SSP1-12 #done` / `#in-progress` | Transitions the issue |

## Team conventions

- `main` is protected — no direct pushes. Open a PR.
- One issue per branch, one branch per PR.
- PR title starts with the issue key so Jira links it.
