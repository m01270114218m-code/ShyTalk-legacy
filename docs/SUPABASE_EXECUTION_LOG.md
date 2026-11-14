# ShyTalk Supabase-only migration — execution log

Target branch: `feature/shytalk-supabase-email-auth-full-migration`

Target project: `pkqcxcfuelubonlkjgoq` (Vox Chat)

## Acceptance criteria

- The Android and iOS clients authenticate only with Supabase Auth.
- Email/password registration, sign-in, password reset, persisted sessions and sign-out work.
- The API verifies Supabase access tokens and preserves authorization checks.
- Profiles, rooms, seats, room presence, chat, private messages, notifications and storage use Supabase.
- Wallet, gifts, VIP, purchases and rewards are server-authorized and atomic.
- LiveKit remains the audio media provider; its token is minted server-side.
- No Firebase SDK, Firebase Admin, FCM, Firebase App Check, Firebase config, emulator, CI action or runtime Firebase reference remains in active app/backend/build configuration.
- Debug and release APK builds and relevant tests pass.

## Phase 0 — verified source inventory (2026-11-15)

This is an audit, not a claim that migration is complete.

| Area | Verified blocker | Required replacement |
|---|---|---|
| Android authentication | `AuthRepositoryImpl.kt` injects `FirebaseAuth`; uses anonymous auth, provider credentials, email links, custom tokens and Firebase token refresh | Supabase Auth email/password, session persistence and recovery |
| Shared auth contract | `AuthRepository.kt` exposes Firebase UID semantics | Supabase UUID identity contract; preserve legacy user mapping explicitly |
| API client | `WorkerApiClient.kt` obtains Firebase ID tokens and Firebase App Check | Supabase access-token provider and server-side JWT verification |
| Dependency injection | `AppKoinModule.kt` constructs Firebase Auth/Firestore and RTDB services/emulators | Supabase client/session wiring and Supabase-backed repositories |
| Android build | `app/build.gradle.kts` applies Google Services; version catalog contains Firebase artifacts | Remove only after all imports and consumers are migrated |
| KMP/iOS | `shared/build.gradle.kts` includes Firebase Android and GitLive Firebase iOS dependencies | Shared Supabase-compatible data/auth layer |
| Express API | Firebase Admin utilities and Firebase-dependent auth middleware/routes | Supabase JWT verification and Postgres/Edge Function operations |
| Realtime | RTDB presence, conversations and typing services | Supabase Realtime Presence/Broadcast/Postgres Changes |
| Push | Firebase Messaging is part of current app configuration | In-app Realtime notifications; choose a separate non-Firebase push transport if native push is required |

## Important compatibility and safety constraints

1. Do not delete Firebase dependencies before migrating every code consumer; that creates a knowingly broken intermediate build.
2. Do not equate a legacy Firebase UID or the app's `uniqueId` with a Supabase Auth UUID. Add a controlled account-linking/mapping process.
3. Never ship Supabase secret/service-role keys or LiveKit signing secrets in the APK.
4. Wallet, gifts, VIP, purchases and rewards must not be writable directly by an untrusted client. Use server-authorized atomic database functions.
5. Supabase Realtime is not a native push delivery service when the app is terminated. Native push requires a non-Firebase provider or must be explicitly deferred.
6. Keep existing screens, assets and feature behavior; migrate their data/service implementations rather than deleting features.

## Execution status

- [x] Confirmed target Supabase project is reachable and active.
- [x] Inspected the public schema; core profile, room, message, wallet, gift, VIP and notification tables exist and RLS is enabled on inspected tables.
- [x] Confirmed inspected application tables currently have zero rows.
- [x] Inspected the Android auth repository, shared auth contract, API client, DI module and KMP dependency configuration.
- [ ] Implement and test Supabase Auth/session layer.
- [ ] Migrate API JWT verification and identity mapping.
- [ ] Migrate each data/realtime/storage/economy repository.
- [ ] Remove Firebase consumers and dependencies across Android, iOS, Express and CI.
- [ ] Run tests and build/inspect APK.

**Current truth:** Phase 0 inspection is complete. Runtime migration has not yet been implemented, and no Firebase removal or successful Supabase-only APK build is claimed.


## Phase 0.1 — Supabase backend inspection (2026-11-15)

The target project also has one deployed Edge Function: `imchat-api` (ACTIVE, version 2, JWT verification enabled).

The deployed handler verifies the caller with `sb.auth.getUser()` and already contains basic handlers for profile, wallet read, room list/create/join/leave, room messages, gift catalog/send RPC, families, follows, blocks, VIP package listing, notifications and agencies.

This is useful backend groundwork, but it is **not proof of a complete app migration**:
- The Android dependency-injection module still constructs Firebase Auth/Firestore and RTDB services.
- The Android API client still obtains Firebase ID tokens.
- The deployed function returns 404 for routes it does not implement; private messaging, seat workflows, storage operations, purchases/VIP activation and several app-specific operations still need route-by-route parity review.
- Existing database tables have no app data yet, so behavior cannot be assumed from schema presence alone.
- The current handler uses a broad CORS origin and needs a security/RLS/authorization review before production reliance.

No production database schema was changed in this inspection step, and no Firebase dependency was removed.


## Phase 1 — Authentication migration: implementation scope confirmed (2026-11-15)

The requested scope is the **entire Firebase-to-Supabase migration**, not enabling both providers. Firebase must remain in place only until every consumer has a working Supabase replacement; it must then be removed from runtime code, platform configuration, Gradle, CI, and the server.

Additional source-level findings:
- The shared `AuthRepository` contract exposes Firebase-specific identity and token concepts (`currentFirebaseUid`, Firebase ID-token refresh, Firebase custom-token and email-link methods).
- Android `AuthRepositoryImpl` is directly coupled to `FirebaseAuth`; it currently bootstraps an anonymous Firebase user on cold start.
- The version catalog already declares Supabase Auth Kotlin SDK `auth-kt` version `3.9.0`, but the Android auth implementation and Koin wiring still use Firebase.
- `WorkerApiClient` obtains and caches Firebase ID tokens and attaches Firebase App Check tokens; this must be migrated as one coordinated contract change, not by swapping only the login SDK.
- The iOS implementation and shared contract are also Firebase-backed, so removing Firebase from Android alone would not meet the requested scope.

**Safety gate:** no Firebase SDK/configuration is to be deleted until Android and iOS auth, API authorization, realtime/presence, storage, push delivery, backend verification, and tests have Supabase-compatible replacements. Existing user identity must be mapped without losing account ownership or wallet balances.

**Implementation status:** source inspection only for this phase. No runtime auth code has been changed yet; no APK/build/test result is claimed.
