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
  new one. Protect `audit_log` with a PostgreSQL `BEFORE UPDATE OR DELETE` trigger raising an
  exception so audit rows are strictly append-only.
- Every write goes through the audit log with the authenticated caller's verified principal.
- Security invariants:
  - Deny by default in Spring Security: only `GET /actuator/health` is public; enforce `@PreAuthorize`
    separating `ROLE_SOURCE_INGEST`, `ROLE_RULE_EDITOR`, `ROLE_PLAN_APPROVER`, and `ROLE_AUDITOR`.
  - Enforce Four-Eyes Segregation of Duties on `POST /api/plans/{id}/apply` (`approver != plan.createdBy`
    and `approver != rule.updatedBy`) with SHA-256 plan digest verification.
  - Never build LDAP DNs or search filters with string concatenation; always use UnboundID typed `DN`,
    `RDN`, and `Filter.create*Filter` APIs (RFC 4514 / RFC 4515), and encrypt connector credentials at
    rest with AES-256-GCM.
  - Never evaluate rule templates or conditions with Spring SpEL (`StandardEvaluationContext`).

## Commits

- Commit in small, focused steps, each one building and passing its tests: for example the migration,
  then the domain model, then the service, then the API, then the tests that cover it. A ticket is
  usually 4 to 8 commits, never one big commit.
- Message: one sentence in the imperative ("Add the person table"), with the issue number at the end
  (`(#2)`). Never `Co-Authored-By`.
- Never commit generated files, build output or secrets.

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
