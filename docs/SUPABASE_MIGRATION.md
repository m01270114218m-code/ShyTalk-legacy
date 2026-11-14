# Supabase migration for ShyTalk

## Scope

Target Supabase project: `gxkdptwkyhwbxyaspagd` (`https://gxkdptwkyhwbxyaspagd.supabase.co`).

This branch is intended to move Android app identity and application data from Firebase Auth / Firestore / Realtime Database to Supabase Auth, Postgres (PostgREST), Realtime and Storage. Firebase Cloud Messaging may remain as a push-delivery transport until a replacement is implemented and verified. LiveKit remains the media plane; token issuance must move behind a trusted server/Edge Function and must never expose LiveKit API secrets in the APK.

## Repository surfaces that must be migrated

- Authentication and account identity: `AuthRepositoryImpl`, auth view models, Google/Apple/email OTP flows, session cache, account linking and sign-out.
- User/profile/device: `UserRepositoryImpl`, `IdentityRepositoryImpl`, `DeviceRepositoryImpl`, age verification and PIN flows.
- Rooms and live state: `RoomRepositoryImpl`, `SeatRequestRepositoryImpl`, `RtdbPresenceService`, `RtdbConversationService`, typing, room moderation and seat state.
- Messaging: `MessageRepositoryImpl`, `PrivateMessageRepositoryImpl`, notification repository and realtime subscriptions.
- Economy: `EconomyRepositoryImpl`, `GiftRepositoryImpl`, wallet ledger, purchase validation, daily rewards, gacha, gift delivery and VIP.
- Assets: `StorageRepositoryImpl`, inventory, equipped cosmetics and signed/private asset delivery.
- Server endpoints: `WorkerApiClient` and the Express API routes must be audited and migrated or deliberately retained. Client-side wallet balance writes are not acceptable.

## Existing schema observations

The target database already contains profiles, rooms, room members/seats/messages, direct conversations/messages, wallets, wallet transactions, gifts, gift transactions, user inventory, notifications and admin/VIP-related tables. Key tables use UUID user IDs tied to `auth.users`.

The Android app currently uses Firebase UIDs and a separate resolved `uniqueId`/cohort identity. These identifiers are not interchangeable. A controlled identity migration and account-linking strategy is required before switching sign-in, or existing accounts may become inaccessible.

## Security gates before production cutover

- Never ship a Supabase secret/service-role key or LiveKit secret in Android.
- Keep wallet, gift, VIP, purchase validation and moderation mutations server-side (RPC/Edge Functions with authorization checks).
- Review the current `wallets` update policy before allowing app traffic: it currently permits an authenticated owner to update their own wallet row. That can permit balance tampering unless the update surface is removed/restricted and all balance changes are ledger-backed server operations.
- Supabase Security Advisor currently reports public/authenticated execution of SECURITY DEFINER functions, including admin and app functions. Review each function's authorization and grants; do not blanket-revoke functions without mapping app dependencies.
- Verify RLS for every exposed table and storage bucket, and verify realtime channel authorization.
- Keep FCM only as a transport if required; remove Firebase Auth/Firestore/RTDB only after their consumers are migrated and tested.

## Migration sequence

1. Freeze schema contract and produce a table/RPC/RLS map against the Android repository models.
2. Add Supabase client, secure per-flavor configuration, and Koin wiring; add connectivity/auth smoke tests.
3. Migrate Auth and establish a stable Supabase UUID identity mapping.
4. Migrate profile, device and app configuration reads.
5. Migrate rooms, seats, membership, presence, typing and room chat realtime.
6. Migrate private messaging and notifications.
7. Migrate assets and inventory/storage access.
8. Migrate wallet, gifts, VIP and purchase flows using server-side atomic operations.
9. Migrate remaining API endpoints and LiveKit token issuance.
10. Remove Firebase Auth/Firestore/RTDB dependencies only after static search, unit/instrumentation tests and end-to-end verification prove no remaining consumers.
11. Run Android build and tests, validate RLS/security advisors, and perform staged cutover with rollback.

## Current status

- [x] Dedicated Git branch created from `build/shytalk-android-apk`.
- [x] Supabase project and existing schema inspected read-only.
- [x] Android Firebase dependency surfaces identified.
- [ ] Supabase SDK/client and app DI wiring.
- [ ] Repository-by-repository migration.
- [ ] Server-side RPC/Edge Function migration and secret configuration.
- [ ] Android build, tests and security verification.

No production tables, users, balances, or existing Edge Functions were modified by this initial inspection.
