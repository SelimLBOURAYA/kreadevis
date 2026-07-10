---
name: lot-test
description: >-
  Guarantees that every lot is covered by written, passing tests before the PR
  is opened, with the JaCoCo coverage gate green (70 % ratchet, target 80 %).
  Use at the end of a lot, before lot-audit and lot-ship, or when the user
  mentions tests, coverage, JUnit or verification.
metadata:
  project: kreadevis-backend
  version: "1.0"
---

# Lot Test — Coverage & quality

Every PR is **preceded** by written, passing tests. The JaCoCo gate fails the
build under **70 %** line coverage (ratchet — target **80 %**, threshold only
goes up).

Mandatory order before any PR:

```
lot-test  →  lot-audit  →  lot-ship
```

---

## 1. Write the tests before opening the PR

Tests are not an optional end-of-lot step — they are part of the definition of
done. Before launching `lot-ship`:

1. Identify the classes and methods **added or modified** in the lot (diff vs `main`).
2. Write the corresponding tests (see section 2).
3. Verify that all tests pass and the coverage gate is green (section 3).
4. Commit the tests in the same lot, on the same branch (`feat/lot-XX-slug`).

Tests are first-class citizens: a lot without tests is an incomplete lot.

---

## 2. What to test per lot

### General rules

- Test **behavior**, not internal implementation.
- Every critical business rule in `CLAUDE.md` gets at least one dedicated test.
- Integration tests use the lot-9 infrastructure: `@SpringBootTest` /
  `@WebMvcTest` / `@DataJpaTest` with **H2**.
- Unit tests (`@ExtendWith(MockitoExtension.class)`) cover isolated business
  logic (services with mocked repositories, computation rules).
- Every new endpoint gets an integration test; every new/modified public
  service method gets a unit test — in the same commit.

### Business-rule test matrix

| Rule | Expected test |
|------|---------------|
| Quote reference `DDMMYY-NNN` with daily sequence | Format + sequence increment test, collision on same day |
| Finalized/cancelled quotes reject item changes | Mutation → `IllegalStateException` → HTTP 409 |
| Totals (HT/VAT/TTC) recomputed on every item change | Totals assertion after add/update/delete item |
| `vatRate` snapshotted on items | Rate change after snapshot does not alter existing items |
| Passwords BCrypt-hashed — never plain in logs/responses | Response body assertion, no hash serialized |
| JWT required on protected routes | 401 without token, 200 with valid token |
| CSV export neutralizes formula injection | Cell starting with `=`, `+`, `-`, `@` prefixed/escaped |
| No legacy view-layer import (JSP/JSTL/Servlets) | Architecture check — no such import compiles |

---

## 3. Measure coverage

### JaCoCo configuration (already wired in `pom.xml`)

The `jacoco-maven-plugin` runs `report` and `check` in the `verify` phase with
a **BUNDLE LINE ratio ≥ 0.70** rule. Excluded from measurement: `config/**`,
`dto/**`, generated `mapper/**`, and `KreadevisBackendApplication`.

> **Ratchet**: the threshold is raised to 0.80 by the dedicated test-coverage
> lot (controllers + security). It is never lowered and never disabled.

### Run the verification

```bash
./mvnw verify
```

`./mvnw verify` runs the tests **and** enforces the JaCoCo threshold. A red
build means failing tests or coverage under the gate — both block the PR.

### Inspect the HTML report

```
target/site/jacoco/index.html
```

---

## 4. Pre-PR checklist

- [ ] Tests written for every class modified in the lot
- [ ] Critical business rules of the lot tested (matrix in section 2)
- [ ] `./mvnw verify` green (passing tests + coverage gate)
- [ ] JaCoCo report inspected — no business class at 0 %
- [ ] Tests committed on the `feat/lot-XX-slug` branch

---

## Absolute rules

- No PR without a green `./mvnw verify`.
- The coverage threshold is a **ratchet**: never lowered, never disabled.
- Never skip or disable a red test — fix the code or remove the feature.
- A test passing thanks to a mocked repository is not an integration test —
  use H2 (`@DataJpaTest`, `@SpringBootTest`) for persistence paths.
