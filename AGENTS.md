# Handoff — Agent Instructions

This repo has a Spring Boot backend (`backend/`) and a React/Vite frontend (`frontend/`).

## Feature Handoff
- Always read `docs/feature-handoff.md` before working on any task.
- Before brainstorming or starting a new feature, ask whether the task is **Frontend**, **Backend**, or **Both**, and stay within that scope unless explicitly told otherwise.
- If a frontend task turns out to need backend support that doesn't exist yet (or vice versa), stop and write the handoff note described in `docs/feature-handoff.md` — don't quietly build the other side yourself unless the task was marked **Both**.

## Java Formatting
- Format every Java file you create or change with the project's formatter (`formatter-maven-plugin`, Eclipse style, configured in `backend/pom.xml`); the Red Hat Java extension in VS Code produces the same style: 4-space indentation, lines wrapped at about 120 columns, and empty bodies written as `{` then `}` on separate lines. Do not copy the indentation of older files; they may predate the formatter.
- After editing Java files, run `cd backend && ./mvnw formatter:format`. Check with `./mvnw formatter:validate`; `./mvnw verify` fails if any Java file is not formatted.
- Do not use Prettier on Java files. Prettier is only used for the frontend (`frontend/`).
