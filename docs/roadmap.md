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
- Backend: `entity/Document`, `entity/DocumentCollaborator` (or similar, for sharing/permissions), `repository/DocumentRepository`, `controller/DocumentController` (create/list/get/delete/rename), `enums/DocumentRole` (owner/editor/viewer)
- Flutter: document list screen, create-document flow, open a document and render its content read-only in `flutter_quill` (load Delta JSON from backend, no editing sync yet)
- Done when: a user can create a document, see it in a list, open it, and see content persisted across app restarts

## Phase 3 — Real-time collaborative editing (the core feature)
- Backend: `com.inkflow.ot` — Delta-based OT transform logic; `websocket/` STOMP config + controllers (`/app/doc/{id}/edit` inbound, `/topic/doc/{id}` outbound); decide and implement snapshot/persistence cadence (see `architecture.md` open decisions)
- Flutter: wire `flutter_quill`'s change stream to STOMP send; subscribe to the doc topic and apply incoming remote ops to the local Quill controller; basic conflict-free local echo (don't re-apply your own op when it bounces back)
- Done when: two clients open the same document and see each other's edits live, without corrupting document state under concurrent typing

## Phase 4 — Presence & polish
- Backend: presence tracking (Redis), maybe cursor position broadcast
- Flutter: show collaborator avatars/cursors, connection status indicator, offline/reconnect handling
- Done when: users can see who else is in the document, and the app recovers gracefully from a dropped WebSocket connection

## Phase 5 — Hardening
- Document history/versioning (restore older snapshot)
- Permission enforcement (viewer can't edit, etc.)
- Error handling, rate limiting, input validation across REST endpoints
- Tests: backend (OT transform unit tests are the highest priority — correctness-critical), Flutter widget tests

## Not yet scheduled
- Offline editing / local-first support
- Export (PDF/Markdown)
- Mobile push notifications

Update the checkboxes and add detail to the *current* phase as work starts; keep future phases high-level until we get there.
