# Rules for every agent working on Asta Identity Hub

Read `docs/spec.md` first: its principles decide design questions.

## How work arrives

Each piece of work is a GitHub issue, implemented on its own branch and delivered as one pull
request that closes the issue. People review and merge; an agent never merges or approves.

## Code

- Java 17, Spring Boot 3.4, Maven wrapper (`./mvnw`). Admin UI in `ui/` (React, Vite, TypeScript).
- Business rules are data. Never hard-code a naming rule, group or target-specific condition in
  Java or TypeScript.
- Connectors contain no business logic: they apply the operation they are given.
- Null is not absent: keep the difference in DTOs, JSON handling, comparisons and tests.
- Flyway migrations: structure only, never data. Never edit a migration that is on `main`; add a
  new one.
- Every write goes through the audit log.

## Tests

- Every change comes with tests. Backend: JUnit 5, Testcontainers PostgreSQL for anything touching
  the database, UnboundID in-memory directory for LDAP. UI: Vitest.
- `./mvnw -B verify` (and `npm --prefix ui run build` once the UI exists) must pass before you
  finish.
- Test data is fictitious (`TEST-0001`, "Ada Test"). No real names, emails or secrets anywhere.

## Don'ts

- No secrets, tokens or personal data in code, tests, logs or commits.
- Don't change `.github/workflows/**` unless the issue asks for it.
- Keep the change to what the issue asks; note follow-ups in the pull request description.
