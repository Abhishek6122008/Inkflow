# API Contract (draft)

Draft only — nothing below is implemented yet. Update this doc whenever a controller or DTO changes shape; it's the source of truth both sides code against.

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
List documents the current user owns or collaborates on.
```json
// Response 200
[
  { "id": "uuid", "title": "string", "ownerId": "uuid", "role": "OWNER|EDITOR|VIEWER", "updatedAt": "iso8601" }
]
```

### POST `/api/documents`
```json
// Request
{ "title": "string" }
// Response 201
{ "id": "uuid", "title": "string", "ownerId": "uuid", "createdAt": "iso8601" }
```

### GET `/api/documents/{id}`
```json
// Response 200
{ "id": "uuid", "title": "string", "content": { /* Quill Delta JSON */ }, "version": 0, "role": "OWNER|EDITOR|VIEWER" }
```
`version` is the OT revision number the client must track and send with subsequent edits.

### PATCH `/api/documents/{id}`
Rename only (content changes go through WebSocket, not REST).
```json
// Request
{ "title": "string" }
// Response 200 — updated document summary
```

### DELETE `/api/documents/{id}`
`204 No Content`. Owner only.

### PUT `/api/documents/{id}/content` (temporary — Phase 1 demo only)
Direct REST save of the full Quill Delta, used only until the WebSocket/OT pipeline (Phase 3) replaces it for live edits.
```json
// Request
{ "content": "Quill Delta JSON, as a string" }
// Response 200
{ "id": "uuid", "title": "string", "content": "string", "version": 1 }
```

### POST `/api/documents/{id}/collaborators`
```json
// Request
{ "email": "string", "role": "EDITOR|VIEWER" }
// Response 200 — updated collaborator list
```

## Real-time — STOMP over `/ws`

Connect with `Authorization` header (or query param, TBD) carrying the JWT; backend validates on `CONNECT`.

### Subscribe: `/topic/doc/{documentId}`
Server broadcasts ops applied to this document to all subscribed clients (including, optionally, an echo/ack to the sender — TBD, see open decision below).

Broadcast payload:
```json
{
  "documentId": "uuid",
  "op": { /* Quill Delta op */ },
  "version": 42,
  "authorId": "uuid"
}
```

### Send: `/app/doc/{documentId}/edit`
Client → server, local edit:
```json
{
  "op": { /* Quill Delta op */ },
  "baseVersion": 41
}
```
Server transforms `op` against any ops applied since `baseVersion`, applies it, increments `version`, and broadcasts the result to `/topic/doc/{documentId}`.

### Open decisions
- Does the server echo the sender's own (possibly transformed) op back on the topic, or does the sender apply it optimistically and only reconcile on conflict? Affects Flutter-side Quill controller wiring in Phase 3.
- Presence channel shape (`/topic/doc/{id}/presence`?) — deferred to Phase 4.
- Error frame shape for rejected ops (e.g. permission denied, stale version) — TBD when `com.inkflow.ot` is implemented.

## Error shape (draft, all REST endpoints)
```json
{ "status": 400, "error": "VALIDATION_ERROR", "message": "string", "timestamp": "iso8601" }
```
