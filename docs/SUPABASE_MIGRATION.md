# Supabase-only migration for ShyTalk

## Target and non-negotiable requirement

Target Supabase project: `gxkdptwkyhwbxyaspagd` (`https://gxkdptwkyhwbxyaspagd.supabase.co`).

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

- [x] Dedicated branch created: `feature/supabase-only-no-firebase`.
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

A successful APK build alone does not prove the backend migration is complete. Completion requires all application paths to work without Firebase.
