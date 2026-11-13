# SSP1-142 — BE3: Conversation API (Backend)

**Jira Key:** SSP1-142  
**Issue Type:** Task  
**Status:** To Do  
**Assignee:** Chuong Hoang Pham  
**Parent Story:** SSP1-121 — Direct Messaging Between Buyer and Seller  
**Sprint:** 5  
**Branch:** `SSP1-142-be3-conversation-api`

---

## Overview

Implement the backend Conversation API that creates or retrieves a buyer-seller conversation for a listing. This is the core backend piece of the conversation page feature (User Story 2: Direct Messaging Between Buyer and Seller).

Related backend tasks in the same story:

| Key | Task | Status |
|-----|------|--------|
| SSP1-142 | BE3 — Conversation API: create or retrieve buyer-seller conversation for a listing | To Do |
| SSP1-135 | BE4 — Message API: send messages and retrieve message history | To Do |
| SSP1-139 | BE5 — Message Authorization: restrict conversation access to participants | To Do |

> **Note:** The original sub-tasks SSP1-127 (BE3), SSP1-132 (BE4), and SSP1-129 (BE5) under SSP1-121 were transitioned to **Done** and removed from active work. The equivalent Task-type issues (SSP1-142, SSP1-135, SSP1-139) in Sprint 5 remain active.

---

## Acceptance Criteria

- `POST /api/conversations` — Create a new buyer-seller conversation for a listing (or return the existing one if it already exists).
- `GET /api/conversations` — List all conversations for the current user (buyer or seller side).
- `GET /api/conversations/{conversationId}` — Retrieve a single conversation by ID.
- A conversation is associated with exactly one listing and exactly two participants (buyer + seller).
- Buyers and sellers cannot start a conversation with themselves.
- Only the two participants of a conversation can access it (see also SSP1-139 / BE5).
- Conversations remain available after the associated listing is sold.
- Return appropriate HTTP status codes (200, 201, 400, 403, 404) and consistent error responses.
- Database tables and relationships are created/migrated (conversations, participants, etc.).
- All new endpoints are covered by unit/integration tests.

---

## Suggested Implementation Steps

1. Define `Conversation` and `ConversationParticipant` JPA entities.
2. Add Flyway migration for the new tables.
3. Create `ConversationRepository` and participant repository.
4. Implement `ConversationService` with create-or-retrieve logic and access checks.
5. Implement `ConversationController` with the endpoints above.
6. Add validation (e.g., buyer ≠ seller, listing exists, school membership).
7. Wire authorization so only participants can read/send in a conversation (align with SSP1-139).
8. Write unit and integration tests for all endpoints.
9. Update API documentation / handoff notes if applicable.

---

## Definition of Done

- [ ] Code implemented and committed to branch `SSP1-142-be3-conversation-api`.
- [ ] Pull request opened against `develop`.
- [ ] All unit/integration tests pass.
- [ ] Jira issue SSP1-142 transitioned to **Done** (or moved to In Progress when work begins).

---

## References

- Jira: https://ldkhoatoank29cltt.atlassian.net/browse/SSP1-142
- Parent story: https://ldkhoatoank29cltt.atlassian.net/browse/SSP1-121
- Related tasks: SSP1-135 (BE4 Message API), SSP1-139 (BE5 Message Authorization)
- Feature docs: `docs/campus_marketplace_buying_messaging_jira_tasks.md` (User Story 2 section)