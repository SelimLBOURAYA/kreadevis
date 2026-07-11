---
name: lot-ship
description: >-
  Guarantees commit uniformity and drives the delivery workflow of a lot:
  Conventional Commits, push to GitHub via gh, PR opening. Use before every
  lot commit, at the end of a lot, or when the user mentions branch, commit,
  push, PR or delivery.
metadata:
  project: kreadevis-backend
  version: "1.0"
---

# Lot Ship — Commits, push, PR

Ships a lot: **uniform** commits, push, PR via **`gh`**. The branch already
exists — its creation and chaining belong to the `sprint` skill (or, for an
isolated lot, to the global lot workflow). Branch = `feat/lot-XX-slug` as
listed in `lots.md` (`chore/` for infra lots). Never commit on `main`.

---

## 1. Commit convention

**Conventional Commits** format, in **English**, imperative mood.

```
<type>(<scope>): <short description>
```

- `<type>`: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`
- `<scope>`: lot number → `(1)`, `(11b)`, `(12)`, …
- `<description>`: imperative, lowercase, no trailing period, max 72 chars

### Valid examples

```
feat(12): split quote totalPrice into HT/VAT/TTC, snapshot vatRate on items
fix(12): map business IllegalStateException to 409 Conflict
test(12): cover status guards, VAT computation, date realignment
feat(7): render quote PDF with HT/TVA/TTC breakdown
chore(10): replace Flyway with Liquibase changelog
docs: sync lots.md status
```

### Anti-examples

| ❌ Forbidden | ✅ Correct |
|-------------|-----------|
| `Ajout du PDF de devis` | `feat(7): render quote PDF` |
| `feat: add stuff` | `feat(13): add pagination to quote list` |
| `feat(lot-12): ...` | `feat(12): ...` (scope = lot number only) |
| French message | English message |
| Commit with a broken build | `./mvnw verify` green before commit |
| Several logical changes mixed | One commit per logical change |

---

## 2. Pre-commit checklist

Before every `git commit`:

- [ ] `./mvnw verify` passes without error
- [ ] No secret, password or token hard-coded in the staged files
- [ ] Commit message follows the convention above
- [ ] Scope = current lot number
- [ ] One single logical change in this commit
- [ ] `docs/audits/lot-XX.md` exists for the current lot (before push)

```bash
./mvnw verify              # build check
git diff --cached --stat   # review what will be committed
git commit -m "feat(XX): <description>"
```

---

## 3. Push and PR opening

### Initial branch push

```bash
git push -u origin feat/lot-XX-<slug>
```

### PR opening with `gh`

```bash
gh pr create \
  --title "feat(XX): <short lot title>" \
  --body "$(cat <<'EOF'
## Summary
- <item 1>
- <item 2>

## Test plan
- [ ] `./mvnw verify` green
- [ ] lot-audit executed (no Critical)
- [ ] No secret committed
EOF
)" \
  --base main \
  --head feat/lot-XX-<slug>
```

Follow-up: `gh pr view`, `gh pr checks`, `gh pr diff`.

**After the PR:** what happens next depends on the calling context — in a
sprint, chain the next lot (see `sprint`); for an isolated lot, stop and wait
for review.

---

## Absolute rules

- Never commit on `main`; `./mvnw verify` green before every commit; never `--no-verify`.
- English message, Conventional Commits, scope = lot number.
- Ship only if `docs/audits/lot-XX.md` exists for the current lot — no audit, no push.
