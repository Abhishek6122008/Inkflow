# API Contract

Auth and Documents (including sharing/roles) below are implemented and verified end to end. Real-time/WebSocket section is still a draft (Phase 3, not built yet). Update this doc whenever a controller or DTO changes shape; it's the source of truth both sides code against.

Base URL: `http://localhost:8080` (dev). All REST responses are JSON. Authenticated endpoints require `Authorization: Bearer <jwt>`.

## Auth — `/api/auth`

### POST `/api/auth/register`
```json
// Request
{ "email": "string", "password": "string", "displayName": "string" }
// Response 201
{ "id": "uuid", "email": "string", "displayName": "string" }
```

### POST `/api/auth/login`
```json
// Request
{ "email": "string", "password": "string" }
// Response 200
{ "accessToken": "jwt", "expiresIn": 86400000, "user": { "id": "uuid", "email": "string", "displayName": "string" } }
```

## Documents — `/api/documents` (all require auth)

### GET `/api/documents`
List documents the current user has access to (owned + shared), each annotated with the caller's role.
```json
// Response 200
[
  { "id": "uuid", "title": "string", "ownerId": "uuid", "updatedAt": "iso8601", "role": "OWNER|EDITOR|VIEWER" }
]
```

### POST `/api/documents`
Creates the document and grants the creator an `OWNER` `DocumentCollaborator` row.
```json
// Request
{ "title": "string" }
// Response 201
{ "id": "uuid", "title": "string", "ownerId": "uuid", "updatedAt": "iso8601", "role": "OWNER" }
```

### GET `/api/documents/{id}`
Requires a `DocumentCollaborator` row for the caller (any role); 404 if none exists (same response as a nonexistent document, to avoid leaking existence).
```json
// Response 200
{ "id": "uuid", "title": "string", "content": "string (Quill Delta JSON)", "version": 0, "role": "OWNER|EDITOR|VIEWER" }
```
`version` is the OT revision number the client must track and send with subsequent edits.

### PATCH `/api/documents/{id}`
Rename only (content changes go through WebSocket, not REST). OWNER only — 403 `DOCUMENT_ACCESS_DENIED` otherwise.
```json
// Request
{ "title": "string" }
// Response 200 — updated document summary (same shape as POST /api/documents response)
```

### DELETE `/api/documents/{id}`
`204 No Content`. OWNER only — 403 `DOCUMENT_ACCESS_DENIED` otherwise.

### PUT `/api/documents/{id}/content` (temporary — Phase 1 demo only)
Direct REST save of the full Quill Delta, used only until the WebSocket/OT pipeline (Phase 3) replaces it for live edits. OWNER or EDITOR only — VIEWER gets 403 `DOCUMENT_ACCESS_DENIED`.
```json
// Request
{ "content": "Quill Delta JSON, as a string" }
// Response 200
{ "id": "uuid", "title": "string", "content": "string", "version": 1, "role": "OWNER|EDITOR" }
```

### POST `/api/documents/{id}/collaborators`
Adds or updates a collaborator's role. OWNER only — 403 `DOCUMENT_ACCESS_DENIED` otherwise. Rejects `role: "OWNER"` (exactly one owner per document, set at creation). 404 if the email doesn't match a registered user.
```json
// Request
{ "email": "string", "role": "EDITOR|VIEWER" }
// Response 200 — full collaborator list for the document
[
  { "userId": "uuid", "email": "string", "displayName": "string", "role": "OWNER|EDITOR|VIEWER" }
]
```

## Real-time — STOMP over `/ws`

Implemented and verified end to end (two concurrent STOMP clients, divergent inserts at the same position, confirmed to converge on identical persisted content).

Connect via `StompConfig.SockJS(url: '.../ws', stompConnectHeaders: {'Authorization': 'Bearer <token>'})`. The JWT is read from the STOMP `CONNECT` frame's native `Authorization` header (not the WebSocket handshake, which can't reliably carry custom headers across all SockJS fallback transports) and validated by `StompAuthChannelInterceptor`, reusing the same `JwtService` logic as REST auth.

### Subscribe: `/topic/doc/{documentId}`
Server broadcasts every applied op to all subscribed clients, **including the original sender** (no optimistic local-apply/reconcile — the sender's own flutter_quill editor already applied the local edit synchronously when the user typed; the client recognizes the echo of its own op by matching `authorId` against its own user id, and on a match only consumes the `version` field, skipping re-application).

Broadcast payload:
```json
{
  "documentId": "uuid",
  "op": [ /* Quill Delta op, list of {insert|retain|delete, attributes?} */ ],
  "version": 42,
  "authorId": "uuid"
}
```

### Send: `/app/doc/{documentId}/edit`
Client → server, local edit:
```json
{
  "op": [ /* Quill Delta op */ ],
  "baseVersion": 41
}
```
Server transforms `op` against every op applied since `baseVersion` (tracked via a bounded 200-entry per-document history), composes the transformed op onto the canonical document state, increments `version`, persists to Postgres, and broadcasts the transformed op + new version to `/topic/doc/{documentId}`. Requires OWNER or EDITOR role — VIEWER submissions are rejected (see error queue below) and never applied or broadcast.

### Errors: `/user/queue/errors`
Per-client private queue. Sent only to the client whose edit was rejected — never broadcast to the topic.
```json
{
  "documentId": "uuid",
  "code": "DOCUMENT_NOT_FOUND | DOCUMENT_ACCESS_DENIED | EDIT_REJECTED",
  "message": "string"
}
```

### Deferred
- Presence channel (`/topic/doc/{id}/presence`?) — deferred to Phase 4.

## Error shape (draft, all REST endpoints)
```json
{ "status": 400, "error": "VALIDATION_ERROR", "message": "string", "timestamp": "iso8601" }
```
