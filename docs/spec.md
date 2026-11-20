# Asta Identity Hub: specification

Status: draft 1, Nov 2026. Built by the Asta Builds software factory (agents implement, people
approve and merge).

## What it is

An identity provisioning hub. A company's HR system says who works there; the hub turns that into
accounts in the systems people need (a directory, email, apps), keeps them in step as people join,
move and leave, and records every change.

```
HR sources ──▶ import ──▶ people (one record per person) ──▶ rules ──▶ plan ──▶ connectors ──▶ targets
 (REST/CSV)       │            change detection, events       (data)    (diff)   (no logic)    (LDAP…)
                  └──────────────────────────── audit log ─────────────────────────────────────┘
```

## Principles (they decide every design question)

1. **HR is the source of truth.** The hub never invents a person; it only reflects sources.
2. **One anchor per person.** Each person is keyed by a stable *anchor* (the source's employee
   number, configurable per source), never by a generated UUID.
3. **Business rules are data, not code.** How an account is named, which groups it gets, when it is
   disabled: all live in rule records an admin edits. Changing a rule needs no deployment.
4. **Connectors write, the hub decides.** A connector only applies an operation the hub computed
   (create, update, disable, delete). It contains no business logic.
5. **Null is not absent.** In an import, a field set to `null` clears it; a field not sent is left
   alone. Every comparison keeps the difference.
6. **Migrations hold structure only.** No data in Flyway migrations; reference data is loaded
   through the API.
7. **Plan, then apply.** Every provisioning run first produces a plan (the operations it would
   perform) that can be reviewed; applying it executes exactly that plan.
8. **Everything is audited.** Imports, rule changes, plans and applied operations are recorded, with
   who and when.
9. **No secrets or personal data in files.** Tests use fictitious people (`TEST-0001`, "Ada Test").

## MVP scope

| # | Feature | Done when |
|---|---|---|
| 1 | Backend skeleton | Spring Boot app starts on PostgreSQL, `/actuator/health` is UP, `mvn verify` runs a Testcontainers test |
| 2 | People and import | `POST /api/sources/{source}/people` upserts a batch by anchor with null-vs-absent semantics; unchanged records are detected by content hash and not rewritten |
| 3 | Change events | every create/update/leave writes a `person.changed` event (with the changed fields) to an outbox table; `GET /api/events` lists them |
| 4 | Rules as data | per-target rules (account id template, attributes, groups, conditions) stored in the database, editable through the API, with a preview endpoint |
| 5 | Connector SPI + LDAP | a `Connector` interface and an LDAP connector (create, update, disable, delete); tested against an in-memory directory |
| 6 | Plan and apply | `POST /api/targets/{target}/plan` computes desired vs actual and returns operations; `POST /api/plans/{id}/apply` executes them through the connector |
| 7 | Audit log | append-only audit records for every write; `GET /api/audit` |
| 8 | Admin UI | React + Vite + TypeScript: people, a person's history, rules editor with preview, plans to review and apply |
| 9 | Local stack | `docker compose up` runs PostgreSQL, an LDAP server and the hub with seed fictitious data |

Out of MVP: SSO for the admin UI (next), more connectors (email, SaaS apps), scheduled syncs,
multi-tenant.

## Stack

Java 17, Spring Boot 3.4, Maven (wrapper), PostgreSQL 17, Flyway, Testcontainers; UnboundID LDAP SDK
for the connector and its in-memory test directory. Admin UI: React 19, Vite, TypeScript,
TanStack Query. CI: GitHub Actions running `./mvnw -B verify` and the UI build.

## Rule model (MVP)

A rule belongs to a target and has:

- `when`: a condition on person fields (e.g. `status == "ACTIVE" and department != null`);
- `accountId`: a template, e.g. `{{lower(ascii(firstName))}}.{{lower(ascii(lastName))}}`, with a
  collision strategy (`append-number`);
- `attributes`: target attribute -> template;
- `groups`: list of templates;
- `onLeave`: `disable` or `delete`, and a grace period in days.

Templates use a small, documented expression language (fields, `lower`, `upper`, `ascii`, `trim`,
`substring`, `coalesce`), evaluated by the hub; no arbitrary code execution.
