# Lot Audit — Lot SB41 — chore/spring-boot-4-1

**Scope:** 1 modified file | **Verdict:** ✅ Ready for PR

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     | 0        | 0       | 1    |
| Performance  | 0        | 0       | 0    |
| Architecture | 0        | 0       | 0    |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info     | `pom.xml:8` | Spring Boot 4.1.0 bumps Spring Security 7.0.x → 7.1.0. No breaking changes affecting this codebase (no custom `SecurityFilterChain` patterns using deprecated APIs). Verified: 106 tests pass, including all `@WebMvcTest` security slices. | Monitor Spring Security 7.1 deprecation notices. |

## Performance
No changes — version bump only.

## Architecture
No changes — version bump only. Layering, DTOs, mappers, Liquibase changesets, entities all untouched.

## Lot-specific notes
- Spring Boot 4.1.0 (released 2026-06-10) — current stable, OSS support until 2027-07-31.
- Upgraded managed dependencies: Spring Framework 7.0.8, Spring Security 7.1.0, Hibernate 7.4.1, Spring Data 2026.0.0.
- `./mvnw verify` green: 106 tests, 0 failures, JaCoCo gate at 70 % satisfied.
- No code changes required — the parent BOM bump was sufficient.

## Recommended next steps
1. Proceed with `lot-ship` (push + PR).
2. Next lot in chain: **Lot 12b** (front unblock: CORS, /users/me, CSV fix).
