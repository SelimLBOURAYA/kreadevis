# kreadevis-backend

Quote-authoring REST API for craftsmen — migration of the legacy `kreadevis`
app (Spring Boot 3 MVC + JSP) to a pure REST API consumed by a separate
Angular frontend (`../kreadevis-frontend`).

## Features

- Client, professional and quote management (CRUD)
- Quote lifecycle with status guards and HT/VAT/TTC totals
- Quote reference `DDMMYY-NNN` with daily sequence
- PDF rendering (OpenPDF) and CSV export (Apache Commons CSV)
- JWT authentication (Spring Security)

## Stack

- Java 25, Spring Boot 4, Maven
- PostgreSQL 17 (Docker) — H2 for tests
- Liquibase migrations, MapStruct mapping

## Prerequisites

- Java 25
- Docker and Docker Compose

## Quick start

```bash
docker compose up -d       # PostgreSQL 17 on :5432
./mvnw spring-boot:run     # API on :8080
```

## Tests

```bash
./mvnw verify              # compile + unit/integration tests (H2) + JaCoCo coverage gate
```

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/app1db` | JDBC URL |
| `DATASOURCE_USERNAME` | `app1user` | DB user |
| `DATASOURCE_PASSWORD` | `app1pass` | DB password (dev default only) |
| `SERVER_PORT` | `8080` | HTTP port |
| `JWT_SECRET` | dev placeholder | JWT signing key — **set a real value outside dev** |
| `DOC_LOGO_PATH` | `classpath:static/logo.png` | Logo used in PDFs |
| `DOC_OUTPUT_DIR` | `/tmp/kreadevis/documents` | Generated documents directory |
| `DOC_FACTURE_DIR` | `/tmp/kreadevis/factures` | Generated invoices directory |
| `CORS_ORIGINS` | `http://localhost:4200` | Allowed frontend origins |

## Project layout

```
src/main/java/com/slim/kreadevis_backend/
├── config/        # Spring configuration
├── controller/    # REST controllers (/api/**)
├── dto/           # request/response records
├── entity/        # JPA entities
├── exception/     # error handling (@RestControllerAdvice)
├── mapper/        # MapStruct mappers
├── repository/    # Spring Data repositories
├── security/      # JWT + Spring Security
└── service/       # business logic
```

Agent conventions: `CLAUDE.md` / `AGENTS.md`. Lot plan: `lots.md`.
