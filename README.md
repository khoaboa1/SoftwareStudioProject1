# Software Studio Project 1

## Setup

```bash
git clone https://github.com/khoaboa1/SoftwareStudioProject1.git
cd SoftwareStudioProject1
./scripts/setup-dev.sh      # or: bash scripts/setup-dev.sh
```

`setup-dev.sh` points git at the repo's hooks and installs the commit-message template.

## Jira workflow

Every branch and commit is tied to a Jira issue so work shows up automatically on the
Jira board (Development panel, commits, branches, PRs).

**Branch naming** — `<type>/<ISSUE-KEY>-<short-description>`

```
feature/SSP-12-user-login
bugfix/SSP-27-fix-null-avatar
chore/SSP-3-ci-pipeline
```

**Commit messages** — start the subject with the issue key:

```
SSP-12 Add password hashing to signup flow
```

The `prepare-commit-msg` hook pulls the key out of your branch name automatically,
so most of the time you just type the message. The `commit-msg` hook rejects commits
with no key.

**Smart Commits** — Jira acts on these keywords in a commit message:

| Syntax | Effect |
|---|---|
| `SSP-12 #comment Fixed the race condition` | Adds a comment to the issue |
| `SSP-12 #time 2h 30m Refactoring` | Logs work against the issue |
| `SSP-12 #done` / `#in-progress` | Transitions the issue |

## Team conventions

- `main` is protected — no direct pushes. Open a PR.
- One issue per branch, one branch per PR.
- PR title starts with the issue key so Jira links it.
