# SentinelCore — Security Audit, Changes, Migration and Rollback

This document records the repository audit performed before the security/cloud enhancement, what
was changed, how the database is affected, and how to roll back. Findings are marked **verified**
(confirmed in source code) or **assumption** (not verifiable from the repository).

## 1. Technology baseline (verified from manifests)

| Area | README claimed | Actually in repository |
| --- | --- | --- |
| Frontend | React 18, MUI v5, React Router v6 | **React 19.2, MUI 9.2, React Router 7.18, Vite 8.1**, Tailwind 4, Recharts 3, Axios 1.18 (JavaScript/JSX) |
| Backend | Java 21, Spring Boot 3.5.5 | Java 21, Spring Boot 3.5.5, Spring Security 6, JPA/Hibernate 6.6, jjwt 0.12.7, springdoc 2.8.5, POI 5.3, OpenPDF 2.0.3 |
| Database | PostgreSQL | PostgreSQL; schema managed by `spring.jpa.hibernate.ddl-auto=update` (no Flyway/Liquibase) |
| Tests | — | Backend: 3 tests (2 `@SpringBootTest` that required a live PostgreSQL, 1 Mockito). Frontend: none |
| CI | — | GitHub Actions: `mvnw clean test` (no DB service, so the context tests could not pass in CI) and `npm ci && npm run build` on Node 20 |
| Deployment | "Nginx reverse proxy" | `frontend/Dockerfile` uses stock `nginx:alpine` with **no custom config** (no API proxy, no SPA fallback). `vercel.json` provides SPA rewrites |
| Cloud | — | **No cloud SDK or connector code existed** |

No dependency versions were upgraded.

## 2. Findings (all verified)

| # | Severity | Finding | Status |
| --- | --- | --- | --- |
| 1 | Critical | `POST /api/auth/register` was public and persisted the client-supplied `role`, so anyone could self-register as `SUPER_ADMIN`. | Fixed: registration disabled by default (`APP_SELF_REGISTRATION_ENABLED`), role always `USER` |
| 2 | Critical | Only `UserController` used `@PreAuthorize`. Any authenticated user (including `USER`) could create/update/delete assets, alerts, incidents, vulnerabilities, patches, risk assessments, threat intel, and read audit logs and reports. | Fixed: permission checks on every endpoint |
| 3 | Critical | JWT signing key hard-coded in `JwtService`; `jwt.secret`/`jwt.expiration` properties were ignored. A second default secret was committed in `application.properties`. | Fixed: key from `JWT_SECRET` (≥32 bytes, validated at startup), ephemeral random key if unset |
| 4 | High | `AuditLoggingAspect` stored `Arrays.toString(args)`; password reset/change endpoints take `Map` bodies, so **plaintext passwords were written to `audit_logs`**. | Fixed: request bodies never serialized; defensive redaction in `AuditLogService` |
| 5 | High | `AdminInitializer` reset 8 accounts to well-known passwords (`admin123`, …) on **every startup**. | Fixed: existing accounts never modified; demo accounts only with `APP_DEMO_USERS_ENABLED=true`; env-based bootstrap admin; startup warning for default passwords |
| 6 | High | `ADMIN` (has `USER_ROLE_ASSIGN`) could promote anyone, including themselves, to `SUPER_ADMIN`; no protection for the last super admin. | Fixed |
| 7 | High | Catch-all exception handler returned `exception.getMessage()` with HTTP 500, which also turned method-security `AccessDeniedException` into 500 and exposed internal messages. | Fixed: typed `ApiResponse` errors, generic 500 with correlation id |
| 8 | High | A live-looking `ORIGINKIT_API_KEY` was committed in `README.md` and `frontend/.env.example`. | Removed from tracked files. **Rotation required** (still in git history) |
| 9 | Medium | No issuer/audience validation; disabled users kept working tokens until expiry. | Fixed |
| 10 | Medium | No brute-force protection; unknown-user and wrong-password responses differed. | Fixed: per-username/IP throttle, uniform message, audit of failures |
| 11 | Medium | Frontend logged the user out on **any** 403, so a single forbidden call (e.g. Incidents page loading `/api/users` as `INCIDENT_MANAGER`) ended the session. | Fixed: only 401 ends the session |
| 12 | Medium | Client-controlled `sortBy` produced 500 on unknown properties. | Fixed: 400 |
| 13 | Medium | `management.endpoint.health.show-details=always`. | Changed to `when-authorized`; `/actuator/health` is public for container health checks |
| 14 | Medium | CORS allows `https://*.vercel.app` with credentials (any Vercel site). Impact limited because auth uses bearer headers, not cookies. | Now configurable via `CORS_ALLOWED_ORIGIN_PATTERNS`; default unchanged to avoid breaking the current deployment. **Narrow it in production.** |
| 15 | Medium | No tenant model anywhere. | New cloud data is tenant-scoped; legacy modules remain single-tenant (see §5) |
| 16 | Low | Bearer token stored in `localStorage` (XSS-exposed). | Not changed (see §6) |
| 17 | Info | `PatchService.applyPatch` only sets `status=APPLIED`; nothing is executed or verified. | Unchanged; documented |
| 18 | Info | Pre-existing `npm audit` advisories in `axios`, `follow-redirects`, `react-router`, `vite` (5 high). | Not changed; run `npm audit fix` after review |

## 3. Implemented controls

**Authentication** — BCrypt (unchanged); JWT HS256 with signature, expiry, issuer (`JWT_ISSUER`) and
audience (`JWT_AUDIENCE`) validation; authorities always reloaded from the database; disabled
accounts rejected per request; login throttling (`APP_LOGIN_MAX_ATTEMPTS`, `APP_LOGIN_LOCK_MINUTES`;
in-memory per instance); uniform login errors; 401 JSON entry point.

**Authorization** — `@PreAuthorize` on every controller method. Mapping (`*_VIEW` for reads,
`*_MANAGE` for writes) mirrors the frontend route guards, so existing pages keep working:

| Module | Read | Write |
| --- | --- | --- |
| Assets | `ASSET_VIEW` | `ASSET_MANAGE` |
| Alerts | `ALERT_VIEW` | `ALERT_MANAGE` |
| Incidents | `INCIDENT_VIEW` | `INCIDENT_MANAGE` |
| Vulnerabilities | `VULNERABILITY_VIEW` | `VULNERABILITY_MANAGE` |
| Risk assessments | `RISK_VIEW` | `RISK_MANAGE` |
| Patches | `PATCH_MANAGE` or `VULNERABILITY_VIEW` | `PATCH_MANAGE` |
| Threat intel | `THREAT_INTEL_VIEW` | `THREAT_INTEL_MANAGE` (new; SUPER_ADMIN, SECURITY_ANALYST) |
| Audit logs | `AUDIT_LOG_VIEW` | — |
| Reports | `REPORT_VIEW` | — |
| Dashboard | any of the module view permissions | — |
| Users list | `USER_CREATE`/`USER_EDIT`/`INCIDENT_MANAGE` (assignee picker) | unchanged + escalation rules |
| Cloud | `CLOUD_ACCOUNT_VIEW` | `CLOUD_ACCOUNT_MANAGE`, `CLOUD_SCAN_RUN`, `REMEDIATION_REQUEST`/`_APPROVE`/`_EXECUTE` |

User-management rules: only `SUPER_ADMIN` can grant `SUPER_ADMIN` or manage/reset/disable a super
admin; nobody can change their own role, disable or delete themselves; at least one active super
admin must remain; role changes need `USER_ROLE_ASSIGN`.

**Audit** — `audit_logs` is append-only in the application (`@Immutable`, `updatable=false`, no
update/delete API). Security events: `LOGIN_SUCCESS/FAILED/BLOCKED`, `ACCESS_DENIED`,
`REGISTER_REJECTED`, user role/status/password events, cloud account/scan events and every
remediation transition including `REMEDIATION_SOD_VIOLATION` and `REMEDIATION_INTEGRITY_FAILURE`.
Security events are written in a separate transaction so they survive a rollback. Details are
sanitized (credential-like values redacted, line breaks stripped, length capped).

## 4. Database changes and migration

The repository's migration convention is Hibernate `ddl-auto=update`; no second migration system was
introduced. All changes are **additive and non-destructive**:

* `users`: new nullable column `tenant_key varchar(64)` (NULL = built-in tenant `default`).
* `audit_logs`: new index `idx_audit_logs_timestamp`.
* New tables: `cloud_accounts`, `cloud_resources`, `cloud_findings`, `cloud_scans`,
  `remediation_requests` (with unique constraints, foreign keys and indexes).

No existing column is altered, dropped or rewritten, and no data is transformed.

**Applying**: start the new backend against the existing database (Hibernate adds the objects).
For change-controlled environments, have a DBA apply the equivalent DDL first and run with
`SPRING_JPA_HIBERNATE_DDL_AUTO=validate`. The exact PostgreSQL DDL can be regenerated offline from
the entity classes (Hibernate `jakarta.persistence.schema-generation.scripts.*` with
`hibernate.boot.allow_jdbc_metadata_access=false`); the additive part is:

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_key varchar(64);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs (timestamp);
-- plus CREATE TABLE cloud_accounts / cloud_resources / cloud_findings / cloud_scans /
-- remediation_requests with their unique constraints, indexes and foreign keys
```

**Known schema caveat**: Hibernate emits `CHECK (col IN (...))` constraints for enum columns and
`ddl-auto=update` never updates them (this is why `AdminInitializer` drops `users_role_check`).
Adding a new enum value later (e.g. a new `RemediationActionType`) requires a manual migration to
drop/recreate the affected check constraint. Adopting Flyway with a baseline of the current schema
is recommended before further schema evolution (requires approval; not done here).

**Recommended database hardening** (operational; not applied automatically):

```sql
-- Run the application with a role that cannot rewrite history
REVOKE UPDATE, DELETE, TRUNCATE ON audit_logs FROM sentinelcore_app;
REVOKE DELETE, TRUNCATE ON cloud_findings, cloud_scans, remediation_requests FROM sentinelcore_app;
```

## 5. Tenant isolation model

* Every cloud table carries `tenant_key`; all cloud repository lookups are tenant-scoped
  (`findByIdAndTenantKey`, tenant predicates in specifications). Records of other tenants are
  reported as **404**, not 403, to avoid confirming their existence.
* The caller's tenant comes only from the authenticated user (`CurrentUserService`), never from the
  request. Only `SUPER_ADMIN` can set a user's `tenantKey`.
* **Limitation**: legacy modules (assets, alerts, incidents, vulnerabilities, risk, patches,
  notifications, audit logs, dashboard) remain single-tenant. Scoping them needs a business decision
  on tenant ownership of existing rows and a backfill migration — deferred, requires approval.

## 6. Deferred / requires decision

* **Token storage**: moving from `localStorage` bearer tokens to HttpOnly `SameSite` cookies needs a
  CSRF strategy and same-site hosting of frontend and API (current Vercel + separate API host is
  cross-site). Recommend after hosting is decided.
* **Enterprise OIDC**: no identity provider exists in the repository; not introduced.
* **Distributed rate limiting**: the login throttle is per instance.
* **Nginx**: no Nginx config exists; adding an API proxy/SPA fallback/security headers config is a
  deployment change left for approval.
* **CI**: frontend tests are not added to CI because CI uses Node 20 while Vitest 5 requires
  Node ≥ 22.12. Backend tests now run in CI without a database.

## 7. Rollback

1. Redeploy the previous backend/frontend build (`git revert` of this change set).
2. The additive schema objects can stay; the old code ignores them. To remove them (destroys cloud
   data and history; requires approval):
   ```sql
   DROP TABLE remediation_requests, cloud_findings, cloud_resources, cloud_scans, cloud_accounts;
   DROP INDEX IF EXISTS idx_audit_logs_timestamp;
   ALTER TABLE users DROP COLUMN IF EXISTS tenant_key;
   ```
3. Note: the old code resets demo-account passwords at startup and has the vulnerabilities above;
   rolling back reintroduces them.

## 8. Actions requiring approval before production use

1. **Rotate the Originkit API key** that was committed (it remains in git history; rewriting history
   needs explicit approval).
2. Set `JWT_SECRET` (≥32 random bytes), and narrow `CORS_ALLOWED_ORIGINS` / `CORS_ALLOWED_ORIGIN_PATTERNS`.
3. Change or disable any account that still uses a well-known default password (the backend logs a
   warning at startup for each one).
4. Decide on tenant ownership for legacy data before onboarding multiple tenants.
5. Review IAM roles (see `docs/cloud-security.md`) before setting `APP_CLOUD_AWS_ENABLED=true`.
6. Enabling `APP_CLOUD_REMEDIATION_EXECUTION_ENABLED=true` permits real changes to cloud resources.
7. Apply the database permission hardening in §4.
8. Review and apply `npm audit fix` for the pre-existing frontend advisories.
