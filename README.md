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

1. Copy `backend/.env.example` to `backend/.env` (gitignored).
2. In the Supabase dashboard, open **Connect → Session pooler** and fill in `DB_URL`
   (JDBC form, `?sslmode=require`), `DB_USERNAME` (`postgres.<project-ref>`) and `DB_PASSWORD`.
3. Run the backend: `cd backend && ./mvnw spring-boot:run`. Flyway creates/updates the schema on startup.

Environment variables with the same names override `.env`.

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
