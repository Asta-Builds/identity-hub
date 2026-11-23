# Asta Identity Hub

An open-source identity provisioning hub: HR data in, accounts out. It keeps directory and app
accounts in step with who works at the company (joiners, movers, leavers), with business rules
kept as data, a reviewable plan before every change, and a full audit trail.

Built by the [Asta Builds](https://github.com/Asta-Builds) software factory: AI agents implement
each ticket on its own branch and open a pull request; CI and the factory's gates check it; a
person approves and merges.

- What it does and why: [`docs/spec.md`](docs/spec.md)
- Rules for agents (and people): [`AGENTS.md`](AGENTS.md)

## Local development

### Prerequisites

- Java 17
- Docker (with Docker Compose)

### Start local PostgreSQL

Start PostgreSQL 17 locally using Docker Compose:

```bash
docker compose up -d --wait
```

Stop it when finished:

```bash
docker compose down
```

### Run the backend

Run the Spring Boot application with the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Datasource configuration uses environment variables with local defaults matching `compose.yaml`:

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/identityhub` |
| `DB_USER` | `identityhub` |
| `DB_PASSWORD` | `identityhub` |

Verify the application is up:

```bash
curl http://localhost:8080/actuator/health
```

Expected response: `{"status":"UP"}`.

### Build and verify

Run the full build and Testcontainers PostgreSQL integration tests (requires Docker):

```bash
./mvnw -B verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -B verify
```

## Status

Specification written; the MVP is being built ticket by ticket (see the issues).

## License

MIT
