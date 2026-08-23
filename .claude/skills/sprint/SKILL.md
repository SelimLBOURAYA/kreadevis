---
name: sprint
description: >-
  Drives the chained delivery of all lots of a sprint, creating each branch
  from the previous lot's branch. Use when the user asks to start or continue
  a full sprint.
metadata:
  project: kreadevis-backend
  version: "1.0"
---

# Sprint — Chained lot delivery

This skill orchestrates the delivery of **all lots of a sprint in sequence**,
without manual intervention between lots, except on explicit blockage.

---

## Branch chaining principle

The first lot of the sprint branches from `main`. Each following lot branches
from the previous lot's branch (not from `main`) — the branch accumulates the
sprint's work until the last PR is merged.

```
main
 └── feat/lot-X-slug            ← first lot of the sprint (base: main)
      └── feat/lot-X+1-slug     ← next lot (base: previous branch)
           └── feat/lot-X+2-slug
                └── ...
```

---

## Per-lot workflow inside the sprint

For each lot of the sprint, in order:

### 1. Create the branch

- **First lot of the sprint**: from `main`
  ```bash
  git checkout main && git pull origin main
  git checkout -b feat/lot-X-<slug>
  ```
- **Following lots**: from the previous lot's branch
  ```bash
  git checkout feat/lot-X-<previous-slug>
  git checkout -b feat/lot-X+1-<slug>
  ```

### 2. Develop the lot

Implement the lot scope as defined in `lots.md`.
`./mvnw verify` green after every commit.

### 3. Apply the skills in order

```
lot-test  →  lot-audit  →  lot-ship
```

- `lot-test`: tests written and passing, coverage ≥ 80 %
- `lot-audit`: consolidated report in **`docs/audits/lot-XX.md`**, security-review executed
  - Critical finding → **stop**, report to the user, wait for their decision
  - Lot recommended for `lots.md` → propose to the user, wait for approval
- `lot-ship`: push + PR via `gh` — **only after** the audit deliverable exists

### 4. Chain without waiting for merge

After the PR is opened, **do not wait for the merge** to start the next lot —
immediately create the next branch from the lot that was just pushed.

---

## Stop conditions

The agent stops and waits for user input in these cases:

| Condition | Action |
|-----------|--------|
| **Critical** finding in audit | Stop the sprint, report the finding, wait for decision |
| `./mvnw verify` red after 2 attempts | Stop, expose the error, wait |
| Coverage < 80 % unreachable within the lot's scope | Stop, propose the missing test cases |
| `lot-audit` recommends a new lot in `lots.md` | Propose the addition, wait for approval before continuing |
| Last lot of the sprint done | Stop, recap the open PRs |

---

## End-of-sprint recap

Once the last lot of the sprint is pushed, produce a recap:

```markdown
## Sprint XX — Recap

| Lot | Branch | PR | Status |
|-----|--------|-----|--------|
| X   | feat/lot-X-slug | #N | Open |
| X+1 | feat/lot-X+1-slug | #N+1 | Open |
| …   | … | … | … |

**Lots waiting for merge:** [list]
**Recommended lots added to lots.md:** [list or none]
**Attention points:** [non-blocking Warning findings to watch]
```

Commit, push and PR conventions: delegated to `lot-ship`. Business and quality
rules: `CLAUDE.md`.
