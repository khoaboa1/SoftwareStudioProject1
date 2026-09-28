# Two-Developer Workflow

## Purpose
Keep backend (Spring Boot) and frontend (React) work aligned by creating a short handoff note before implementation, instead of one side silently guessing what the other needs.

## Before Starting Work
Ask one question first:

- **Frontend**
- **Backend**
- **Both**

Do this before brainstorming or starting a new feature.

## For New Backend Features
Create a concise markdown note for the frontend developer that includes:

- feature summary
- API endpoints (method + path)
- request shape
- response shape
- auth or session needs (e.g. must be logged in, uses the `studentId` session)
- validation rules
- success and error states (status codes + error message shape)
- any UI constraints or assumptions

Keep it short and practical.

## For New Frontend Features
Create a concise markdown note for the backend developer that includes:

- feature summary
- user flow
- required data from the backend
- expected API needs (endpoints, shapes, if not already built)
- loading, empty, and error states
- permission or auth assumptions
- any layout or interaction constraints

Keep it short and practical.

## Scope Rules
If the task is frontend-only, stay on frontend only.

- Frontend work may use mock/sample data to build and test the UI.
- Frontend work should not add real backend code (new Spring controllers, services, entities).
- Remove any mock data after the real endpoint exists and is wired up.

If the task is backend-only, stay on backend only.

- Backend work may use a quick manual test (curl, MockMvc test) instead of building UI.
- Don't build throwaway frontend UI for backend-only tasks — this repo's frontend is small enough that a backend dev can verify via tests/curl.

Do not cross into the other side unless the task is explicitly marked **Both**.

## For Debugging
Use the bug description to decide whether it is:

- **Frontend**
- **Backend**
- **Both**

If the description is unclear, ask a clarifying question before making changes.

## Goal
Make it easy for the frontend developer to build confidently against a known API shape, and keep backend changes documented in a simple handoff format so nothing silently drifts out of sync.

---

## Handoff Note: SSP1-55 Verification Status & Zombie Account Protection

### Feature Summary
Each student now has an explicit `verified` boolean stored in the database. Unverified accounts cannot log in directly and are sent a fresh 6-digit verification PIN whenever they attempt to sign in. Accounts that remain unverified for 30 minutes are automatically deleted from the database. Once verified, the student is never asked for a PIN on subsequent logins.

### API Endpoints
- `POST /auth/signup`
  - **Request Shape**: `{ "studentName": "Jane Doe", "email": "jane@tulane.edu", "password": "Password123!" }`
  - **Response Shape (201)**: `{ "id": 1, "studentName": "Jane Doe", "email": "jane@tulane.edu", "verified": false, "emailVerified": false, ... }`
  - **Duplicate Handling**: If the email is already verified, returns `409 Conflict`. If an existing registration with that email is still unverified within 30 minutes, it refreshes the registration and sends a new PIN (invalidating the prior PIN). If older than 30 minutes, the zombie account is deleted and registered freshly.
- `POST /auth/login`
  - **Request Shape**: `{ "email": "jane@tulane.edu", "password": "Password123!", "deviceId": "browser-uuid" }`
  - **Response Shape (200 - Unverified)**:
    ```json
    {
      "requiresPin": true,
      "email": "jane@tulane.edu",
      "message": "Account is unverified. A new verification PIN has been sent to your school email."
    }
    ```
  - **Response Shape (200 - Verified)**:
    ```json
    {
      "requiresPin": false,
      "id": 1,
      "studentName": "Jane Doe",
      "email": "jane@tulane.edu",
      "verified": true,
      "emailVerified": true,
      "message": "Login successful"
    }
    ```
  - **Expired Unverified Response (401 Unauthorized)**:
    ```json
    {
      "message": "Verification expired after 30 minutes. Please sign up again."
    }
    ```
- `POST /auth/verify-pin`
  - **Request Shape**: `{ "email": "jane@tulane.edu", "pin": "123456", "deviceId": "browser-uuid" }`
  - **Response Shape (200)**: Student entity with `"verified": true` and `"emailVerified": true`. Session established.
- `GET /auth/me`
  - **Response Shape (200)**: Student entity with `"verified": true`, `"emailVerified": true`.

### Auth & Session Needs
- Sessions are only established upon successful `/auth/verify-pin` or when `/auth/login` succeeds for an already verified student (`requiresPin: false`).
- Unverified logins do not create a session.

### UI Constraints or Assumptions
- When `POST /auth/login` returns `{ "requiresPin": true }`, redirect or display the PIN verification view so the student can enter the newly sent code.
- If `POST /auth/login` returns `401` with `"Verification expired after 30 minutes. Please sign up again."`, redirect to `/signup` with a message.

---

## Handoff Note: SSP1-67 Implement Profile Creation API

### Feature Summary
Authenticated students can generate their student profile record (`POST /api/profiles`). The backend automatically parses the student's authenticated email, extracts the school domain (e.g., `tulane.edu` from `jane@tulane.edu`), and securely stores it in the `school_domain` database column. This domain acts as the tenant identifier for future marketplace scoping. Duplicate profiles for the same student are prevented, and any manual `studentId` or `schoolDomain` parameters injected into the request body are strictly ignored.

### API Endpoints
- `POST /api/profiles`
  - **Auth**: Requires an active session established via `/auth/login` or `/auth/verify-pin` (uses `studentId` session attribute).
  - **Request Headers**: `Content-Type: application/json`
  - **Request Shape**:
    ```json
    {
      "name": "Jane Doe",
      "major": "Computer Science",
      "bio": "Junior studying CS and Math."
    }
    ```
  - **Validation & Field Constraints**:
    - `name`: String, required, max 255 characters, cannot be blank.
    - `major`: String, required, max 255 characters, cannot be blank. Accommodates predefined university majors or custom/manual entries.
    - `bio`: String, optional, max 1000 characters.
  - **Success Response (201 Created)**:
    ```json
    {
      "id": 1,
      "name": "Jane Doe",
      "major": "Computer Science",
      "bio": "Junior studying CS and Math.",
      "schoolDomain": "tulane.edu",
      "studentId": 1,
      "createdAt": "2026-09-22T00:00:00.000000"
    }
    ```
  - **Error Responses**:
    - `401 Unauthorized`: Returned when the user has no active session or is unauthenticated.
      ```json
      { "message": "User must be authenticated to create a profile." }
      ```
    - `400 Bad Request`: Returned when required fields (`name`, `major`) are missing/blank, or character limits are exceeded.
      ```json
      { "message": "Name is required" }
      ```
    - `409 Conflict`: Returned if the authenticated user already has an existing profile.
      ```json
      { "message": "A profile already exists for this account." }
      ```

### Auth & Session Needs
- Relies on Spring session cookie (`JSESSIONID`). Must be authenticated.

### UI Constraints or Assumptions
- Frontend can present a dropdown of popular Tulane University majors with an "Other" option allowing manual text entry. Both flow into the `major` request field.
- If the endpoint returns `409 Conflict`, the frontend can redirect the student to view/edit their existing profile.
- If `401 Unauthorized` is returned, redirect the user to `/login`.

---

## Handoff Note: SSP1-68 Implement Profile Retrieval API

### Feature Summary
Authenticated students can fetch their own student profile record (`GET /api/profiles/me`). The backend uses the active session (`studentId` session attribute) to determine which profile to return — no ID is passed in the URL, preventing cross-user access. The response includes the `schoolDomain` extracted from the student's email during profile creation, which the frontend uses for marketplace eligibility scoping. If the student has not yet created a profile, a `404 Not Found` is returned to signal they should be redirected to the create-profile flow.

### API Endpoints
- `GET /api/profiles/me`
  - **Auth**: Requires an active session established via `/auth/login` or `/auth/verify-pin` (uses `studentId` session attribute).
  - **Request Headers**: None required (session cookie sent automatically).
  - **Success Response (200 OK)**:
    ```json
    {
      "id": 1,
      "name": "Jane Doe",
      "major": "Computer Science",
      "bio": "Junior studying CS and Math.",
      "schoolDomain": "tulane.edu",
      "studentId": 1,
      "createdAt": "2026-09-22T00:00:00.000000"
    }
    ```
  - **Error Responses**:
    - `401 Unauthorized`: Returned when the user has no active session, or the session cookie is invalid/expired.
      ```json
      { "message": "User must be authenticated to create a profile." }
      ```
    - `404 Not Found`: Returned when the authenticated user has not yet created a profile.
      ```json
      { "message": "Profile not found" }
      ```

### Auth & Session Needs
- Relies on Spring session cookie (`JSESSIONID`). Must be authenticated.
- No JWT token is used — "token" in the acceptance criteria maps to the session cookie.

### UI Constraints or Assumptions
- If `404 Not Found` is returned, the frontend should redirect the student to the profile creation page.
- If `401 Unauthorized` is returned, redirect the user to `/login`.
- The `schoolDomain` field is used by the frontend to filter marketplace listings to the student's university.

---

## Handoff Note: SSP1-69 Enforce IDOR Protection on Profile Endpoints

### Feature Summary
Strict Insecure Direct Object Reference (IDOR) protection has been implemented for targeted profile endpoints (`GET /api/profiles/{id}` and `PUT /api/profiles/{id}`). A student can only view or modify their own profile record. Attempting to view or update another student's profile ID is physically blocked and returns `403 Forbidden`. If a requested profile ID does not exist, the API returns `404 Not Found`.

### API Endpoints
- `GET /api/profiles/{id}`
  - **Auth**: Requires an active session (`studentId` in HTTP session).
  - **Path Parameter**: `id` (Long) - the profile's internal ID.
  - **Success Response (200 OK)**:
    ```json
    {
      "id": 1,
      "name": "Jane Doe",
      "major": "Computer Science",
      "bio": "Junior studying CS and Math.",
      "schoolDomain": "tulane.edu",
      "studentId": 1,
      "createdAt": "2026-09-22T00:00:00.000000"
    }
    ```
  - **Error Responses**:
    - `401 Unauthorized`: User has no active session.
      ```json
      { "message": "User must be authenticated to create a profile." }
      ```
    - `403 Forbidden`: Authenticated student does not own this profile (Cross-User Fetch IDOR prevention).
      ```json
      { "message": "You do not have permission to access this profile." }
      ```
    - `404 Not Found`: Profile ID does not exist.
      ```json
      { "message": "Profile not found" }
      ```

- `PUT /api/profiles/{id}`
  - **Auth**: Requires an active session (`studentId` in HTTP session).
  - **Path Parameter**: `id` (Long) - the profile's internal ID.
  - **Request Headers**: `Content-Type: application/json`
  - **Request Shape**:
    ```json
    {
      "name": "Jane Smith",
      "major": "Data Science",
      "bio": "Updated bio text."
    }
    ```
  - **Validation & Field Constraints**:
    - `name`: String, optional. Null or blank values leave the existing value unchanged.
    - `major`: String, optional. Null or blank values leave the existing value unchanged.
    - `bio`: String, optional, max 1000 characters.
    - Note: `schoolDomain` and `studentId` cannot be updated via this endpoint (they are immutable from the client).
  - **Success Response (200 OK)**:
    ```json
    {
      "id": 1,
      "name": "Jane Smith",
      "major": "Data Science",
      "bio": "Updated bio text.",
      "schoolDomain": "tulane.edu",
      "studentId": 1,
      "createdAt": "2026-09-22T00:00:00.000000"
    }
    ```
  - **Error Responses**:
    - `401 Unauthorized`: User has no active session.
      ```json
      { "message": "User must be authenticated to create a profile." }
      ```
    - `403 Forbidden`: Authenticated student attempts to update another user's profile (Cross-User Modification IDOR prevention).
      ```json
      { "message": "You do not have permission to modify this profile." }
      ```
    - `404 Not Found`: Profile ID does not exist.
      ```json
      { "message": "Profile not found" }
      ```
    - `400 Bad Request`: Validation failure (e.g. exceeds character limit) or malformed JSON body.
      ```json
      { "message": "bio: Bio cannot exceed 1000 characters" }
      ```

### Auth & Session Needs
- Requires standard session cookie (`JSESSIONID`).
- Checks profile owner's `student.id` against the active session's `studentId`.

### UI Constraints or Assumptions
- When viewing or editing profile settings, the frontend should handle `403 Forbidden` by displaying an unauthorized access warning or navigating back to the student's own profile (`/api/profiles/me`).
- If `404 Not Found` occurs on edit, prompt the student to create their profile.



