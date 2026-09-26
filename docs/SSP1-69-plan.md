# SSP1-69: Profile IDOR protection plan

Scope: Backend only. Preserve the existing implementation and branch history.

## Base and history

Use `SSP1-68-implement-profile-retrieval-api` as the dependency base while PR #24
is open. It supplies profile retrieval and the shared not-found handling. Target
this branch for a focused PR; retarget to `develop` after #24 is merged.

Git records the following events on September 25, 2026 (America/Chicago, CDT):

- 14:30:23: task branch created at `9108e8e`, the SSP1-68 retrieval commit.
- 14:39:21: `bef56be` added targeted GET/PUT ownership checks and IDOR tests.
- 14:46:21: `2b7004f` added endpoint documentation and controller Javadocs.

Both implementation commits were authored by Chuong Pham. The requested branch
already exists, so rerunning `git checkout -b` would fail; continue on it.

## Execution plan

1. Review session identity resolution, ownership checks before reads/mutations,
   request DTOs, and exception-to-status mappings.
2. Verify cross-user GET and PUT return 403, and rejected PUT leaves the owner's
   persisted profile unchanged. Verify unauthenticated requests return 401 and
   injected ownership/domain fields cannot change profile identity.
3. Run the full backend Maven test suite, inspect the diff, and reconcile the
   frontend handoff with actual endpoint behavior.
4. Commit with the SSP1-69 issue key, push the existing branch, and open a PR
   using `.github/pull_request_template.md`. Leave Jira status unchecked unless
   independently verified.

## Acceptance criteria

- User A fetching User B's profile ID receives 403 (404 is also permitted).
- User A updating User B's profile ID receives 403 without changing its data.
- Owners retain working fetch/update access; session identity controls ownership.

PUT is the supported update method; this task does not introduce PATCH.

## Verification result

September 25, 2026: `backend/mvnw.cmd test` passed all 62 tests with zero
failures, errors, or skips, including both IDOR acceptance cases, persistence
checks after rejected updates, unauthenticated requests, and ownership injection.
Existing authorization code satisfies the acceptance criteria; this review added
regression coverage and corrected the handoff's update validation description.
