# Roadmap

Both backend and Flutter are currently empty scaffolds (backend: `.gitkeep` packages only; Flutter: default counter app). Phases below are ordered so each one is independently testable before the next starts.

## Phase 0 — Foundations (current state)
- [x] Backend skeleton: Spring Boot project, package structure, `application.properties` (Postgres/Redis/JWT/WebSocket config placeholders)
- [x] Flutter deps installed (dio, retrofit, riverpod, flutter_quill, stomp_dart_client, go_router, secure_storage, etc.)
- [ ] Postgres + Redis running locally (docker-compose or local install) — not yet verified
- [ ] `docs/api-contract.md` and `docs/data-model.md` kept in sync as build proceeds (this doc set)

## Phase 1 — Auth
- [x] Backend: `entity/User`, `repository/UserRepository`, `security/` (`JwtService`, `JwtAuthFilter`, `AppUserDetailsService`, `SecurityConfig`), `controller/AuthController` (`register`, `login`), `dto/RegisterRequest`, `LoginRequest`, `AuthResponse`, `UserResponse`, `exception/GlobalExceptionHandler` — verified end to end against live Postgres (register → login → JWT issued; duplicate email → 409; bad password → 401; unauthenticated request → 403)
- [ ] Flutter: login/register screens, `dio` interceptor attaching JWT, `flutter_secure_storage` for token persistence, `go_router` route guarding
- Done when: register → login → authenticated request round-trip works end to end (backend half done; Flutter half remaining)

## Phase 2 — Document CRUD (no real-time yet)
- [x] Backend: `entity/Document`, `repository/DocumentRepository`, `controller/DocumentController` (create/list/get/delete/rename via PATCH+DELETE, plus a temporary `PUT .../content` for the Phase 1 demo) — verified end to end (create → rename → delete → confirmed 404)
- [x] Flutter: document list screen (`document_list_screen.dart`) with create/open/rename/delete, parameterized `/documents/:id` route, editor screen opens any document by id (no longer hardcoded to one doc)
- [x] Backend: `entity/DocumentCollaborator`, `enums/DocumentRole` (OWNER/EDITOR/VIEWER), `POST /api/documents/{id}/collaborators` — access resolved via collaborator membership (404 if none), role enforced per action (VIEWER blocked from edits, only OWNER can rename/delete/manage collaborators) — verified end to end (share as VIEWER → blocked from edit/rename/delete; upgrade to EDITOR → edit succeeds, rename/delete still blocked; non-collaborator → 404)
- [x] Flutter: role surfaced in document list (`role` badge per doc), rename/delete/share actions hidden for non-owners, document editor renders read-only (`readOnly: true`, toolbar hidden) for VIEWER role
- Done when: a user can create a document, see it in a list, open it, and see content persisted across app restarts — met, including sharing/roles.

## Phase 3 — Real-time collaborative editing (the core feature)
- [x] Backend: `com.inkflow.ot` — `Operation`/`DeltaIterator`/`Delta` (a verified line-for-line port of `dart_quill_delta`, so server transform/compose is bit-for-bit identical to the Flutter client's); `websocket/` STOMP config + `StompAuthChannelInterceptor` (JWT on CONNECT) + `DocumentEditController` (`/app/doc/{id}/edit` inbound, `/topic/doc/{id}` outbound, `/user/queue/errors` for rejected edits); persists on every applied op; see `architecture.md` "Real-time sync" for the full resolved design
- [x] Flutter: `lib/api/document_sync_service.dart` wraps `stomp_dart_client` (connect with JWT, subscribe to doc topic + error queue, send local ops); `document_screen.dart` listens to `controller.document.changes` for local edits and applies remote ops via `controller.compose(op, selection, ChangeSource.remote)`; own-op echo recognized via `authorId` match (version-only consumption, no re-apply) rather than a send-side optimistic-skip
- Done when: two clients open the same document and see each other's edits live, without corrupting document state under concurrent typing — **verified**: two real STOMP clients sent divergent concurrent inserts at position 0 against the same `baseVersion`; both converged on identical op order, and the persisted Postgres content matched the expected result exactly (`"HelloWorld\n"`)
- Gap: no JUnit suite for `Delta`/`Operation` transform/compose yet (only an ad-hoc manual sanity check + the live two-client test above) — tracked under Phase 5 testing

## Phase 4 — Presence & polish
- Backend: presence tracking (Redis), maybe cursor position broadcast
- Flutter: show collaborator avatars/cursors, connection status indicator, offline/reconnect handling
- Done when: users can see who else is in the document, and the app recovers gracefully from a dropped WebSocket connection

## Phase 5 — Hardening
- Document history/versioning (restore older snapshot)
- ~~Permission enforcement (viewer can't edit, etc.)~~ — done in Phase 2 (`DocumentCollaborator`/`DocumentRole`)
- Error handling, rate limiting, input validation across REST endpoints
- Tests: backend (OT transform unit tests are the highest priority — correctness-critical), Flutter widget tests

## Not yet scheduled
- Offline editing / local-first support
- Export (PDF/Markdown)
- Mobile push notifications

Update the checkboxes and add detail to the *current* phase as work starts; keep future phases high-level until we get there.
