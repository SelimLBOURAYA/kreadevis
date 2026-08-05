# Lot Audit — Lot 12b — feat/lot-12b-front-unblock

**Scope:** 8 modified files (+228 / -19) | **Verdict:** ✅ Ready for PR

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     | 0        | 0       | 2    |
| Performance  | 0        | 0       | 0    |
| Architecture | 0        | 0       | 0    |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info     | `config/CorsConfig.java:14` | `allowed-origins` split by comma with `.trim()` — a trailing space in env var (e.g. `http://a, http://b`) would produce `" http://b"` which CORS won't match. This is a known Spring Boot CORS pitfall — documented in lot 16 hardening notes. | Hardening (stricter parsing, allowCredentials per-origin) deferred to lot 16 per spec. |
| Info     | `controller/UserController.java:26` | `GET /api/users/me` exposes user roles — necessary for front RBAC but confirms that `ROLE_ADMIN` is visible client-side. No privilege escalation risk (JWT is signed server-side, not trust-on-client). | Acceptable — roles are needed for conditional UI rendering. |

## Performance
No regression. `findByLogin` is a single indexed lookup. CSV import parsing overhead unchanged (same number of passes).

## Architecture
- **Layering**: Controller → Service → Repository respected on all new code.
- **Constructor injection**: `CorsConfigurationSource` injected in `SecurityConfig`, `CorsConfig` uses `@Value`.
- **DTOs**: `UserResponse` already carries roles — reused as-is for `/me`.
- **No entity exposure**: `GET /api/users/me` returns `UserResponse` (DTO).
- **CSV**: `setHeader()` without arguments infers from file — columns accessed by configured name, not position. Required column validation with explicit 400.
- **No Liquibase changeset** needed — no schema change.

## Lot-specific notes
- **CORS**: `CorsConfigurationSource` bean wired on `app.cors.allowed-origins`. SecurityConfig calls `.cors(cors -> cors.configurationSource(...))`. Allowed methods: GET, POST, PUT, DELETE, OPTIONS. Allowed headers: `*`. `allowCredentials: true`. Hardening (restricted methods/headers, prod origins via env var) stays in lot 16.
- **`/users/me`**: Returns `UserResponse` (id, login, email, roles) from `SecurityContextHolder`. JWT subject = login → `findByLogin`. 401 if unauthenticated, 404 if user deleted.
- **CSV fix**: Header now inferred from file. Required columns (`label`, `unitPrice`, `vatRate`) validated — missing → `IllegalArgumentException` → 400. Optional columns use `isMapped()` guard. `warnings` field kept but empty (reserved for lot 8b).

## Recommended next steps
1. Proceed with `lot-ship` (push + PR).
2. Next lot in chain: **Lot 13** (pagination).
