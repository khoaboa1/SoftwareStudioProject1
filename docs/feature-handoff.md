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
