# Supabase-only migration for ShyTalk

> **Active target (verified 2026-11-15):** Supabase project `m01270114218m-code's Project` (`pkqcxcfuelubonlkjgoq`) in organization `فوكس شات`, region `eu-west-3` (Paris). This is the user's currently active project. The schema currently contains the app's main social tables, but all inspected application tables returned zero rows. This branch is an in-progress migration; it is not yet Firebase-free and must not be released until all phases and tests pass.


## Target and non-negotiable requirement

Target Supabase project: `pkqcxcfuelubonlkjgoq` (`https://pkqcxcfuelubonlkjgoq.supabase.co`).

The target architecture is Supabase-only for application backend services. Firebase must be removed from the Android app, Express API, build configuration, CI, local development, tests, and runtime configuration. No Firebase Auth, Firestore, Realtime Database, Storage, Cloud Messaging, App Check, Firebase Admin SDK, Firebase emulators, Google Services Gradle plugin, or Firebase configuration files may remain in the shipped application or active project configuration.

LiveKit remains the real-time audio/video media plane; it is not a Firebase dependency. LiveKit secrets and access-token signing remain server-side.

## Important implementation constraint: push delivery

Supabase provides Auth, Postgres, Realtime, Storage, and Edge Functions, but it is not itself a native Android push-delivery network equivalent to FCM. If Firebase is prohibited entirely, push delivery needs a separately selected non-Firebase transport (for example a self-hosted UnifiedPush-compatible distributor or another non-Firebase provider). Until that transport is implemented and tested, the app must use in-app Supabase Realtime notifications and must not silently retain FCM.

## Existing repository dependencies confirmed

- Android `app/build.gradle.kts` applies the Google Services Gradle plugin.
- Android version catalog pins the Firebase BOM and Firebase Auth, Firestore, Realtime Database, Messaging and GitLive Firebase libraries.
- `AuthRepositoryImpl` uses Firebase Auth for anonymous sign-in, email-link, Google/Apple credentials, persisted sessions and sign-out.
- `IdentityRepositoryImpl` uses Firebase Auth and refreshes Firebase ID tokens.
- `WorkerApiClient` attaches Firebase ID tokens to protected Express API requests and Firebase App Check tokens where available.
- `AppKoinModule`, `MainActivity`, session cache/cold-start code and numerous repositories/services are wired to Firebase.
- `express-api/package.json` includes `firebase-admin` and Firebase rules testing dependencies.
- `express-api/src/utils/firebase.js` initializes Firestore, Auth, RTDB and Messaging and configures Firebase emulators.
- Express authentication middleware verifies Firebase ID tokens, resolves Firebase UID to the legacy `uniqueId`, and applies suspension, ban, cohort and admin gates.
- Auth and user routes use Firestore documents and Firebase Admin Auth.
- Existing Supabase schema contains `profiles` keyed to `auth.users.id` plus rooms, room membership/seats/messages, conversations, wallets, wallet transactions, gifts, gift transactions, inventory, notifications and other social/admin tables.

## Identity compatibility

The existing app uses Firebase UIDs plus a separate stable `uniqueId`/cohort identity. These identifiers are not interchangeable. The migration must preserve legacy user identity and authorization semantics, including device binding, bans, suspensions, cohorts, admin checks, account linking, age/legal gates and session restoration. Existing accounts require an explicit, secure account-claim/linking process; do not silently create duplicate profiles for existing users.

## Replacement architecture

- Authentication: Supabase Auth (email/password as requested; any social provider must be configured directly with Supabase Auth).
- Identity/profile: `auth.users.id` UUID plus a server-managed legacy `uniqueId` mapping in Postgres.
- API authorization: validate Supabase access JWTs server-side using the project's supported signing/JWKS configuration; never trust decoded, unverified tokens.
- App data: Postgres/PostgREST with RLS and narrowly scoped RPCs.
- Live state, presence, typing and room events: Supabase Realtime (Broadcast/Presence/Postgres Changes) with explicit channel authorization.
- Assets: Supabase Storage with private buckets and signed URLs where appropriate.
- Server-side privileged workflows: Supabase Edge Functions or a retained non-Firebase API service using a Supabase server client. Service-role/secret keys must never be shipped in Android.
- Voice media: LiveKit; mint short-lived room tokens only on a trusted server/Edge Function.
- Native push: choose and implement a non-Firebase push transport, or explicitly accept in-app notifications only.

## Security requirements

- Never ship Supabase secret/service-role keys or LiveKit API secrets in Android.
- Wallet balances, gifts, VIP, purchases, rewards, moderation and admin mutations must be atomic and server-authorized; clients must not update balances directly.
- Review the existing wallet update policy and all SECURITY DEFINER function grants. Do not blanket-grant or blanket-revoke without mapping dependencies and tests.
- Verify RLS for every exposed table, storage bucket and realtime channel.
- Preserve fail-closed behavior for identity, bans, suspensions, cohort and admin authorization.
- Keep audit trails for all privileged economy and moderation actions.

## Required migration phases

1. Inventory every Firebase symbol, dependency, configuration, service, rule, emulator, CI secret and test fixture across the full repository.
2. Map every Firestore collection/document, RTDB path, Auth custom claim, FCM topic/token and Storage object to its Supabase replacement and migration procedure.
3. Complete Supabase Auth, secure session persistence, email/password UI, password reset, sign-out and account recovery.
4. Migrate the Express/API trust boundary to Supabase JWT validation and preserve identity/ban/cohort/admin checks.
5. Migrate profile, device, identity linking, age verification, PIN and legal/onboarding flows.
6. Migrate rooms, seats, membership, moderation, presence, typing and realtime room chat.
7. Migrate private messages, conversations and notification inbox.
8. Migrate Storage, inventory, equipped assets and signed/private asset delivery.
9. Migrate wallets, ledger, gifts, VIP, rewards and purchases using server-side atomic operations.
10. Replace Firebase App Check with an independently verified Play Integrity strategy; replace FCM with a selected non-Firebase push transport or remove remote push while retaining in-app notifications.
11. Migrate local development, test suites, CI/CD, secrets, release configuration and documentation away from Firebase.
12. Remove Firebase SDKs, plugins, config files, service-account use and all Firebase code only after the replacement for each consumer is merged and tested.
13. Run static searches proving no Firebase references remain in active source/build configuration, then run unit, integration, security, Android debug and release builds.
14. Validate existing account continuity, room functionality, realtime, economy invariants, RLS and push behavior before declaring completion.

## Current status

- [x] Dedicated branch created: `feature/shytalk-supabase-vox-chat`.
- [x] Supabase project and current schema inspected read-only.
- [x] Confirmed the current Android and Express backend are still Firebase-dependent.
- [x] Documented the Supabase-only target and migration gates.
- [ ] Supabase Auth/client/session implementation.
- [ ] Server JWT and legacy identity migration.
- [ ] Repository-by-repository data and realtime migration.
- [ ] Storage, economy, VIP and LiveKit token migration.
- [ ] Non-Firebase push decision and implementation.
- [ ] Removal of all Firebase dependencies/configuration.
- [ ] Full test suite, security review and APK build.
- [ ] Production cutover and legacy-account verification.

**Implementation status:** no application runtime code has been migrated yet on this branch; the current commits are migration documentation and inventory only. A successful APK build alone does not prove the backend migration is complete. Completion requires all application paths to work without Firebase.

## Repository-wide inventory update (2026-11-15)

The branch tree contains 3,019 tracked files. The first recursive path inventory confirms Firebase is also present outside the initially inspected Android/Express files:

- Root configuration: `.firebaserc`, `firebase.json`, and Firebase-related scripts/configuration.
- CI/local actions: Google Services decoding, Firebase rules deployment, and Firebase emulator startup actions under `.github/actions/`; workflows and local scripts also reference emulator/rules setup.
- Shared Kotlin multiplatform module: `shared/build.gradle.kts` still includes `firebase-common`, Firebase Auth and App Check on Android, and GitLive Firebase App/Auth/Firestore/Database on iOS.
- Android build: `app/build.gradle.kts` still applies Google Services and defines Firebase RTDB URLs and Firebase-oriented local emulator settings. The version catalog still declares the Firebase BOM, native Firebase artifacts, App Check, GitLive artifacts and Firebase-related coroutine integration.
- Express API: Firebase Admin remains a runtime dependency; Firebase rules testing remains a development dependency. The Firebase utility initializes Firestore, Auth, RTDB and Messaging. Auth routes issue Firebase custom tokens and use Firestore for OTP/PIN/biometric state.
- Android auth boundary: `AuthRepositoryImpl`, `IdentityRepositoryImpl` and `WorkerApiClient` are still directly coupled to Firebase Auth tokens. The shared/iOS auth implementations also need migration.
- No tracked path containing “supabase” was found outside the migration document in this recursive path inventory. This is a path-name observation, not proof that no inline Supabase code exists; content-level search is still required.

### Migration gate clarified

Do not remove Firebase Gradle aliases, SDKs, CI actions, emulator configuration or server packages as an isolated cleanup: current application code imports and calls them, and doing so would knowingly break builds or runtime paths. Replace each consumer and its tests first, then remove its dependency in the same verified migration phase. The current tree shows this is a cross-platform migration (Android + iOS + Express + CI), not an Android-only dependency swap.

### Newly confirmed status

- [x] Recursive repository path inventory obtained (3,019 tracked files).
- [x] Confirmed Firebase usage in Android, shared Android, iOS, Express, root configuration and CI/local actions.
- [ ] Content-level exhaustive symbol/consumer map and per-service replacement matrix.
- [ ] Supabase client initialization and session/auth implementation on Android and iOS.
- [ ] Remaining migration phases listed above.



## Verified source-code audit (2026-11-15)

The following files were inspected on `feature/shytalk-supabase-vox-chat`; they still contain active Firebase runtime dependencies and are migration blockers:

- `app/src/main/java/com/shyden/shytalk/data/repository/AuthRepositoryImpl.kt`: FirebaseAuth, anonymous sign-in, Google/Apple credentials, email-link auth, custom tokens, token refresh, sign-out.
- `app/src/main/java/com/shyden/shytalk/core/di/AppKoinModule.kt`: FirebaseAuth, Firestore, Realtime Database emulator setup, RTDB presence/conversation/typing services.
- `shared/src/commonMain/kotlin/com/shyden/shytalk/data/repository/AuthRepository.kt`: public contract documents Firebase UID/token semantics.
- `shared/src/commonMain/kotlin/com/shyden/shytalk/data/repository/SessionCache.kt`: cache schema and account binding are explicitly Firebase-UID based.
- `app/src/main/AndroidManifest.xml`: Firebase Installation ID metadata, Firebase Messaging service, Firebase OAuth redirect handlers.
- `app/build.gradle.kts`: Google Services Gradle plugin remains applied.
- `shared/build.gradle.kts`: Firebase Auth dependency remains.
- `gradle/libs.versions.toml`: Firebase BOM, Google Services plugin, GitLive Firebase and Supabase Kotlin SDK entries remain.
- `express-api/src/utils/firebase.js` and `express-api/src/routes/auth.js`: backend Firebase Admin Auth/Firestore/RTDB/Messaging paths remain (confirmed in earlier inventory).

### Migration order required to avoid a broken intermediate build

1. Add a single Supabase client/config module using only the publishable/anon key in the Android client; keep secret/service-role keys server-side.
2. Replace AuthRepository implementation and session cache with Supabase Auth session/UUID semantics; preserve anonymous auto-entry without a login screen. Add an explicit legacy-account linking strategy before changing identity keys.
3. Replace WorkerApiClient Firebase ID-token/App Check handling with Supabase access-token validation on the API (JWKS or Supabase Auth user verification).
4. Replace RTDB presence, room state, messages, conversations and typing with Supabase Realtime/Postgres channels and reconnect handling.
5. Move storage operations to Supabase Storage with bucket policies.
6. Move wallet, gifts, VIP, purchases and rewards to server-side atomic RPC/Edge Functions; never permit client-side balance writes.
7. Replace FCM with a separately configured non-Firebase push transport, or explicitly retain in-app Realtime-only notifications while the app is foreground/background-connected.
8. Remove Firebase from Android, iOS, Express, CI, local emulator scripts, tests, Gradle catalog, manifests and repository configuration only after all consumers are migrated.
9. Run static Firebase-reference scans, unit/integration tests, security advisors, and a clean Android release build; inspect the produced APK before calling the migration complete.

**Current verified result:** no application runtime migration has been committed to this branch yet. The latest commit only corrected this migration document. The successful Android workflow currently visible in the repository belongs to a different branch (`feature/supabase-full-integration`) and therefore is not evidence that this branch builds or that Firebase has been removed.


## Live target-project audit (2026-11-15)

The target project `pkqcxcfuelubonlkjgoq` was queried directly:

- Project is reachable at `https://pkqcxcfuelubonlkjgoq.supabase.co`.
- Core schema is present: profiles, wallets, rooms, room_members, chat_messages, reactions, gifts, gift_transactions, families, follows, blocks, wallet_transactions, agencies, VIP packages/subscriptions, notifications and agency join requests.
- All inspected application tables currently contain zero rows; no production user/content data was found in those tables during this audit.
- RLS is enabled on all inspected public application tables. Security advisor returned no findings at audit time.
- Existing Realtime publication includes `public.room_members` and `public.chat_messages`.
- Storage bucket `imchat-avatars` exists with owner-scoped insert/update/delete policies.
- Auth user trigger `on_auth_user_created` calls `handle_new_auth_user()`, which is intended to provision profile/wallet records on signup.
- Edge Function `imchat-api` is ACTIVE (version 2, JWT verification enabled). Its current routes include profile, wallet, room listing/create/join/leave, chat message read/write, gifts/send via `send_gift`, families, follows, block list, VIP package listing, notifications and agencies.
- The function does not yet expose a complete set of app APIs: observed gaps include VIP purchase/entitlement mutation, wallet top-up/payment verification, profile editing/search, room moderation/seats/voice permissions, family management beyond create/join, agency join-request workflow, and a dedicated presence/typing service. Its current permissive CORS origin also needs to be restricted before production.
- Publishable key is configured in Android build variants. This key is public by design and is safe only with correct RLS; no service-role key was placed in the APK.

### Critical remaining client/server cutover

Although Supabase project schema and one Edge Function are already present, ShyTalk's active Android runtime still constructs FirebaseAuth in `AuthRepositoryImpl.kt`, `WorkerApiClient.kt`, and Koin DI. The app's API base URLs still point to the legacy Express service, and the Express auth routes still import Firebase Admin. The app therefore does **not** yet authenticate against this Supabase project and cannot be called Supabase-only.

The Android build already declares `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY` BuildConfig fields, and the version catalog declares `supabase-auth-kt`; however, these declarations are configuration only and are not proof of a wired client. The shared module does not yet declare the Supabase client plugins needed for PostgREST/Realtime/Storage in its common source set.

Do not remove Firebase Gradle plugins or server packages until all app and server call sites have been migrated and verified. In particular, the existing Firebase-based account identity and uniqueId mapping must be migrated without silently creating duplicate users. Guest auto-entry must use Supabase anonymous Auth, with a real persisted Auth session and owner-scoped RLS; it must not bypass Auth or expose tables to unauthenticated users.
