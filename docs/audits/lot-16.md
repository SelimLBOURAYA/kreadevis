# Lot Audit — Lot 16 — feat/lot-16-security-hardening

**Scope:** 37 modified files (23 main, 14 test) | **Verdict:** ⚠️ Fix warnings (0 Critical)

Note: the `security-review` subagent type is not available in this
environment. This audit was performed manually against the checklist in
`skill/lot-audit/checklists.md`.

## Summary
| Dimension    | Critical | Warning | Info |
|--------------|----------|---------|------|
| Security     | 0        | 2       | 2    |
| Performance  | 0        | 1       | 0    |
| Architecture | 0        | 0       | 1    |

## Security
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Warning | `security/RateLimitFilter.java` | CORS preflight `OPTIONS` requests to `/api/auth/**` consume a slot in the same bucket as the actual `POST`. A browser's first cross-origin login/register (before the preflight is cached for `maxAge=3600`) burns 2 of the 5 allowed requests instead of 1. | Not fixed — report only per lot-audit rules. Recommend excluding `OPTIONS` from the rate limiter (`if ("OPTIONS".equals(request.getMethod())) { filterChain.doFilter(...); return; }`) in a follow-up commit. |
| Warning | `security/RateLimitFilter.java` | Rate-limit key is `request.getRemoteAddr()` only — behind a reverse proxy/load balancer this is always the proxy's IP, collapsing every client into one bucket (documented as a known limitation in `security.md` §3, not new to this lot, but worth flagging since it's now live code, not just a plan). | No action for this lot — `server.forward-headers-strategy` + trusted-proxy config is out of scope until the app is actually deployed behind a proxy. |
| Info | `entity/RefreshToken.java` | Raw refresh tokens are hashed with plain SHA-256 (no salt) before storage. Acceptable here because the token itself is a 256-bit `SecureRandom` value (unlike a user-chosen password), so a salt adds no defense against precomputed-table attacks — but call out for future readers who might assume SHA-256 alone is always wrong. | No action — documented in `security.md`'s "Écarts" note. |
| Info | `service/impl/AuthServiceImpl.java` | `refresh()` rotates the token (revokes the presented one, issues a new pair) rather than just checking validity — stronger than the lot's minimum requirement ("stocké en DB, hashé, révocable"). | No action — intentional hardening beyond spec. |

Verified during this audit:
- `JWT_SECRET` and `DATASOURCE_PASSWORD` have no default in `application.yaml`; `JwtProperties` is `@Validated` with `@NotBlank @Size(min = 64)` on `secret` — confirmed via `JwtPropertiesTest` that a blank or short secret fails validation.
- `git grep -i "changeme\|app1pass"` → no hits in `src/main`.
- `.env.example` lists every env var referenced by `application.yaml` (`JWT_SECRET`, `DATASOURCE_*`, `CORS_ORIGINS`, `COMPANY_*`, `MAILJET_*`, `EMAIL_*`).
- CORS: `allowedOrigins` from `CORS_ORIGINS` (no default `*`), `allowedHeaders` restricted to `Authorization`/`Content-Type` (was `*`), `allowCredentials=true`, `maxAge=3600`.
- `@Valid` present on every new/changed controller input (`RefreshRequest`, `RegisterRequest`).
- BCrypt cost factor explicit at 12 (`new BCryptPasswordEncoder(12)`); `RegisterRequest.password` now `@Size(min = 12)`.
- `JwtAuthFilter`: `UsernameNotFoundException` from a stale JWT subject is caught and short-circuited to `401` via `response.sendError` — does not reach `GlobalExceptionHandler`'s generic 500 path. Verified by `JwtAuthFilterTest`.
- `RefreshToken` is never returned over HTTP — only the raw token string (via `AuthResponse`, a DTO) crosses the boundary; the entity itself stays server-side.
- Removed the dead `/actuator/**` `permitAll()` matcher (`SecurityConfig`) — actuator isn't on the classpath today, so this was previously a latent full-exposure risk if it's ever added without someone re-auditing `SecurityConfig`.
- Canonical identity: `AuthServiceImpl`, `UserDetailsServiceImpl`, `UserDetailsImpl` all now key on `email` (unique, validated) instead of the previous inconsistency (login for auth, email for lookup).
- CSV upload: extension allowlist (`.csv`), content-type allowlist (`text/csv`, `application/vnd.ms-excel`), empty-file rejection, row cap (`app.csv.import.max-rows`), `spring.servlet.multipart.max-file-size=2MB` → `MaxUploadSizeExceededException` mapped to `413` in `GlobalExceptionHandler`. Verified end-to-end (real embedded server, not MockMvc — see Lot-specific notes) via `CsvUploadLimitsIntegrationTest`.
- No hard-coded secret, no password/token logged, no legacy view-layer import introduced.

## Performance
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Warning | `security/RateLimitFilter.java` | `buckets` is a `ConcurrentHashMap<String, Bucket>` with no eviction — every distinct IP that ever calls `/api/auth/**` leaves a permanent entry for the process lifetime. On a public-facing instance under sustained traffic (or trivially, an attacker cycling source IPs) this is unbounded memory growth. | Not fixed — report only. Recommend a bounded cache with TTL (e.g. Caffeine, already implicit via Bucket4j's own `Bucket4j.builder()` extensions, or a periodic sweep) in a follow-up if this ships behind a public endpoint before lot 17 (Dockerization). |

- No N+1 introduced: `AuthServiceImpl.refresh()` does one `findByTokenHash` read, one revoke-save, one issue-save — flat, no loops.
- `RefreshToken` write paths (`login`/`register`/`refresh`) are all inside the existing `@Transactional` boundary of `AuthServiceImpl.refresh()`; `login`/`register` are non-transactional (unchanged from before this lot) but each only performs its own sequential saves — no partial-write risk introduced by this lot beyond what already existed.
- CSV row-count cap is checked per-record during the existing streaming loop — no extra buffering added.

## Architecture
| Severity | Location | Finding | Action |
|----------|----------|---------|--------|
| Info | `security/RateLimitFilter.java` | Reads its limits via `@Value(...:default)` instead of a `@ConfigurationProperties` record (the pattern used everywhere else in this codebase, e.g. `JwtProperties`, `CsvImportProperties`). Deliberate: `@ConfigurationProperties` beans aren't resolved inside `@WebMvcTest` slices, and this filter — unlike a plain service — is auto-detected by those slices regardless of component scanning, so a `@ConfigurationProperties` constructor dependency breaks every existing `@WebMvcTest` class with a `NoSuchBeanDefinitionException`. | No action — documented as an intentional deviation in `security.md` and `lots.md`. |

- Layering respected: no business logic in controllers; `RateLimitFilter`/`JwtAuthFilter` live in `security/`, config records in `config/`.
- `RefreshToken` follows the existing entity convention (`@Getter @Setter @EqualsAndHashCode(of = "id")`, no `@Data`) and carries its own rich-domain check (`isUsable()`) rather than pushing that logic into `AuthServiceImpl` — consistent with `CONVENTIONS.md` §1.
- New Liquibase changeset (`006-refresh-tokens.yaml`), no edit to a merged one.
- DTOs (`RefreshRequest`, updated `AuthResponse`) stay in `dto/auth/`; no entity crosses the controller boundary.
- No legacy view-layer import found.
- Tests co-located with the code they cover, added/updated in the same commits.

## Lot-specific notes

- **CSV oversized-upload test required a real embedded server.** `MockMvc`'s mock request does not exercise the servlet container's multipart size enforcement (`spring.servlet.multipart.max-file-size`), so `CsvUploadLimitsIntegrationTest` uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` with the JDK's built-in `HttpClient` (no new test-scoped HTTP client dependency) to hit the real port and observe the actual `413`.
- **Rate-limit isolation across shared test contexts.** `QuoteReadIntegrationTest`, `CsvUploadLimitsIntegrationTest` and `AuthRefreshIntegrationTest` all use `@SpringBootTest` + `@ActiveProfiles("integration-test")` with an identical configuration signature, so Spring's test context cache reuses the *same* `RateLimitFilter` singleton — and its in-memory bucket — across all three classes. The `integration-test` profile raises `app.rate-limit.auth.capacity` to 1000 so their cumulative `/api/auth/**` calls (login + refresh across several `@Test` methods) don't trip each other's bucket; `RateLimitFilterTest` exercises the real 5/min production default directly against the filter, unaffected by this override.
- **`QuoteReadIntegrationTest` fix.** Its `tearDown()` previously deleted `users` before the (now-existing) `refresh_tokens` FK reference, causing a referential-integrity failure the moment `login()` in `setUp()` started issuing real refresh tokens. Fixed by deleting `refresh_tokens` first.
- **Existing test fallout from the password-length bump.** `AdminUserControllerTest` and `AuthControllerTest` used 11–14-char passwords for their "happy path" assertions; bumped to comply with the new `min = 12` — not a scope violation, a direct consequence of item 7.
- `./mvnw verify`: green, 201 tests, JaCoCo gate passed (see `lot-test` step). Re-ran twice to rule out flakiness from the shared rate-limit bucket described above.
- **Decisions confirmed with the user before implementation** (see conversation): Bucket4j in-memory (no Redis infra in this project), refresh token backed by a new dedicated table with rotation, HIBP breach-check explicitly out of scope, canonical identity aligned on `email`.

## Recommended next steps
1. Merge this PR once reviewed.
2. (Optional, not blocking) Exclude `OPTIONS` from `RateLimitFilter` so CORS preflight doesn't consume login/register quota.
3. (Optional, not blocking) Add eviction/TTL to `RateLimitFilter`'s in-memory bucket map before this ships behind a long-lived public instance (natural fit for lot 17 — Dockerization/publication, where the app first runs unattended for extended periods).
4. (Optional, not blocking) If the app is ever deployed behind a reverse proxy/load balancer, revisit IP-based rate-limit keying (`server.forward-headers-strategy` + trusted proxy allowlist) — tracked already in `security.md` §3.
