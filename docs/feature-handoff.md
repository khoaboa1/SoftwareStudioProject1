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

