---
name: dep-update
description: >-
  Checks and updates the Maven dependencies of the kreadevis-backend project (Spring Boot
  parent, plugins, third-party libraries) before opening a PR. Use when the
  user asks for a dependency update, wants to ensure the project is current,
  or before writing a lot PR.
metadata:
  project: kreadevis-backend
  version: "1.0"
---

# Dep Update — Maven dependency refresh

Checks dependency status, proposes and applies **safe** updates, then
validates the build before commit. Two levels: **patch/minor** (applied
automatically) and **major** (proposed, never applied without explicit
approval).

---

## Workflow

```
Task Progress:
- [ ] Step 1 — Outdated report
- [ ] Step 2 — Candidate analysis
- [ ] Step 3 — Apply safe updates
- [ ] Step 4 — Build verification
- [ ] Step 5 — Report and optional commit
```

---

### Step 1 — Outdated report

Generate the outdated-dependency report without touching `pom.xml`:

```bash
./mvnw versions:display-dependency-updates versions:display-plugin-updates \
  -DprocessDependencyManagement=false 2>&1 | tee /tmp/dep-report.txt
```

Extract from the report:

- Dependencies with a patch/minor update available (e.g. `1.2.3` → `1.2.5` or `1.3.0`).
- Dependencies with a major update available (e.g. `2.x` → `3.x`).
- The Spring Boot parent if outdated.

---

### Step 2 — Candidate analysis

For each candidate, evaluate:

| Criterion | Rule |
|-----------|------|
| **Patch** (`x.y.Z`) | Apply without asking — minimal risk |
| **Minor** (`x.Y.z`) | Apply unless the dependency is managed by the Spring Boot BOM |
| **Major** (`X.y.z`) | Propose to the user — **never apply without approval** |
| **Spring Boot parent** | Always propose — never bump without approval |
| **BOM-managed dependencies** | Do not override the version in `pom.xml`; note that the BOM manages it |

> **Spring Boot BOM note**: if the dependency is listed in the Spring Boot BOM
> (e.g. `spring-*`, `hibernate-*`, `jackson-*`, `liquibase-*`, `postgresql`, `h2`), do not pin an explicit version in `pom.xml` — the parent
> manages it. Version bumps happen through the parent update.

---

### Step 3 — Apply safe updates

For **patch** and **minor** updates outside the BOM:

```bash
# Patch only
./mvnw versions:use-latest-releases \
  -DallowMajorUpdates=false \
  -DallowMinorUpdates=false \
  -DgenerateBackupPoms=false

# Or patch + minor
./mvnw versions:use-latest-releases \
  -DallowMajorUpdates=false \
  -DgenerateBackupPoms=false
```

Read `pom.xml` before and after to verify only the expected versions changed.
If a BOM-managed version was overridden, remove it manually from `pom.xml`.

---

### Step 4 — Build verification

After each `pom.xml` change:

```bash
./mvnw verify
```

- Green build → continue.
- Red build → identify the culprit dependency, **revert it**
  (`git checkout pom.xml`), document it in the report.

---

### Step 5 — Report and optional commit

Produce a synthetic report:

```markdown
## dep-update report — [date]

### Applied updates
| Dependency | Old | New | Type |
|------------|-----|-----|------|

### Proposed updates (major / parent)
| Dependency | Old | Available | Blocker |
|------------|-----|-----------|---------|

### BOM-managed dependencies (untouched)
| Dependency | Current version (BOM) |
|------------|----------------------|

### Build
- `./mvnw verify`: ✅ green / ❌ red (details below)

### Failed updates (reverted)
| Dependency | Reason |
|------------|--------|
```

**Commit:** if updates were applied and the build is green, propose:

```bash
git add pom.xml
git commit -m "chore(XX): update patch/minor dependencies"
```

Replace `XX` with the current lot id. Wait for user confirmation before
committing.

---

## Rules

- Never bump the **Spring Boot parent** without explicit approval.
- Never apply a **major** update without explicit approval.
- Never commit a `pom.xml` with a red build.
- Never pin an explicit version for a BOM-managed dependency.
- Always read `pom.xml` before applying changes.
- Unknown or dubious dependency → stop and ask.

## Resources

- Project conventions: `CLAUDE.md`, `lots.md`
- Maven Versions plugin: `versions:display-dependency-updates`, `versions:use-latest-releases`
