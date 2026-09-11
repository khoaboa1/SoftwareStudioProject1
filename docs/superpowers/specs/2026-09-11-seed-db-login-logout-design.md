# Seed Database + Login/Logout Design

Status: Approved
Branch: `SSP1-53-Implement-login-logout`

## Goal

Wire up the existing login/signup UI skeleton (frontend) to a real
persisted-user backend so a developer can seed test students and exercise a
full login → session persists → logout flow. No marketplace features, no
dashboard/landing page (tracked separately) — this is auth + persistence
only.

## Out of scope

- Any marketplace feature (listings, search, claims).
- A post-login landing/dashboard page (separate task).
- Password reset, email verification, JWT/stateless auth.
- DB migrations tooling (Flyway/Liquibase) — `ddl-auto=update` is fine at
  this stage.

## Database

- PostgreSQL, run locally via Docker Compose (`docker-compose.yml` at repo
  root): db/user/password all `handoff`, port `5432`, named volume for
  persistence across restarts.
- `backend/src/main/resources/application.properties` points at it
  (`jdbc:postgresql://localhost:5432/handoff`).
- `spring.jpa.hibernate.ddl-auto=update`.

## Data model

`Student` becomes a JPA `@Entity` (table `students`):

| field         | type            | notes                              |
|---------------|-----------------|-------------------------------------|
| id            | Long            | `@Id @GeneratedValue`               |
| studentName   | String          |                                      |
| email         | String          | `@Column(unique = true)`            |
| passwordHash  | String          | `@JsonIgnore` — never serialized    |
| sellingItems  | List\<String\>  | `@ElementCollection`                |
| dormLocation  | String          | nullable                            |

`StudentRepository extends JpaRepository<Student, Long>` adds
`Optional<Student> findByEmail(String email)`.

`StudentService` is updated to read from the repository instead of the
hardcoded in-memory list currently in
`backend/src/main/java/com/Handoff/backend/service/StudentService.java`.

## Seed data

A `CommandLineRunner` bean seeds the four existing demo students (Sarah,
Alex, Chloe, Brian — same names/emails/dorms as today's hardcoded
`StudentService` list) into the DB on startup, only if the table is empty.
All seeded students get the same test password: `Password123!`
(BCrypt-hashed before insert). This lets anyone testing login use e.g.
`sarah@tulane.edu` / `Password123!`.

## Auth mechanism

No full Spring Security authentication filter chain / `UserDetailsService`
— that machinery (roles, authorities, auth providers) isn't needed for this
scope. We only take a `PasswordEncoder` (`BCryptPasswordEncoder`) bean from
`spring-boot-starter-security`, plus a minimal `SecurityConfig` that:

- disables CSRF (dev/test scope, JSON API),
- permits all requests (authorization is enforced manually per-endpoint by
  checking `HttpSession`, not by Spring Security),
- configures CORS to allow `http://localhost:5173` with credentials, since
  session cookies require that for the browser to send/receive them
  cross-origin.

Login state lives in the servlet `HttpSession`: on successful login we
store the student's id as a session attribute; logout invalidates the
session.

## API endpoints (`AuthController`, base path `/auth`)

- `POST /auth/signup` — body `{ studentName, email, password }`.
  - Validates email matches `.edu` pattern and password is ≥ 8 chars
    (mirrors `frontend/src/lib/validation.ts`).
  - 409 if email already registered.
  - Hashes password, saves student, returns the created student (no
    password) with 201.
- `POST /auth/login` — body `{ email, password }`.
  - Looks up by email, compares BCrypt hash.
  - On success: stores `studentId` in session, returns the student (no
    password).
  - On failure (no such email, or bad password): 401 with a generic
    "Invalid email or password" message — never reveal which part was
    wrong.
- `POST /auth/logout` — invalidates the current session, 204.
- `GET /auth/me` — returns the logged-in student from session, or 401 if
  there is none.

`GET /students` (existing endpoint) keeps working, now backed by the DB,
and never leaks `passwordHash` (via `@JsonIgnore`).

## Frontend

- `frontend/src/lib/api.ts`: small fetch wrapper defaulting to
  `credentials: 'include'` and base URL `http://localhost:8080`, so session
  cookies flow both ways.
- `LoginPage`: submit calls `POST /auth/login`.
  - On success: replace the form with a "Logged in as {studentName}" state
    and a **Logout** button. Logout calls `POST /auth/logout` and resets
    back to the form. No new route — the login/dashboard split is a
    separate task.
  - On failure: existing `FormAlert` shows the server's error message.
- `SignupPage`: submit calls `POST /auth/signup`; keep the existing
  success/error `FormAlert` UI, just wire it to the real response instead
  of the `setTimeout` stub.

## Testing plan

1. `docker compose up -d` to start Postgres.
2. Run the backend (`./mvnw spring-boot:run`) — seed runs on first boot.
3. Run the frontend (`npm run dev`).
4. Log in as `sarah@tulane.edu` / `Password123!` → see logged-in state.
5. Refresh the page mid-session (manually re-check `/auth/me` or similar)
   to confirm the session persists server-side.
6. Log out → confirm form reappears and `/auth/me` now 401s.
7. Attempt login with a wrong password → confirm generic 401 error shown.
8. Sign up a brand-new `.edu` email → confirm it can then log in; sign up
   again with the same email → confirm 409 is surfaced.
