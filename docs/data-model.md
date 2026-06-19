# Data Model (draft)

Draft only — nothing below is implemented yet. Update alongside actual `entity/` and `enums/` classes; this is the source of truth for schema intent.

## Entities (Postgres, JPA)

### User — `entity/User`
| field | type | notes |
|---|---|---|
| id | UUID | PK |
| email | String | unique, not null |
| passwordHash | String | bcrypt, never returned in DTOs |
| displayName | String | |
| createdAt | Instant | |

### Document — `entity/Document`
| field | type | notes |
|---|---|---|
| id | UUID | PK |
| title | String | |
| ownerId | UUID | FK → User |
| content | JSON / TEXT | latest persisted Quill Delta snapshot |
| version | long | OT revision counter, incremented per applied op |
| createdAt | Instant | |
| updatedAt | Instant | bumped on every persisted edit/snapshot |

Snapshot cadence (how often `content`/`version` get written vs. living only in Redis/memory) is an open decision — see `architecture.md`.

### DocumentCollaborator — `entity/DocumentCollaborator`
| field | type | notes |
|---|---|---|
| id | UUID | PK |
| documentId | UUID | FK → Document |
| userId | UUID | FK → User |
| role | DocumentRole (enum) | OWNER / EDITOR / VIEWER |
| addedAt | Instant | |

Composite unique constraint on `(documentId, userId)`.

### DocumentSnapshot — `entity/DocumentSnapshot` (Phase 5 — history)
| field | type | notes |
|---|---|---|
| id | UUID | PK |
| documentId | UUID | FK → Document |
| content | JSON / TEXT | full Delta at this point |
| version | long | matches `Document.version` at snapshot time |
| createdAt | Instant | |

Deferred until Phase 5 unless history/undo is needed earlier.

## Enums — `enums/`

### DocumentRole
`OWNER`, `EDITOR`, `VIEWER`

Permission rules (enforce in `service/` layer, not just controller):
- `VIEWER`: read-only, cannot send edit ops over WebSocket
- `EDITOR`: read + edit, cannot delete document or manage collaborators
- `OWNER`: full control, exactly one per document (the creator)

## Redis (not JPA entities, but part of the data model)

- `doc:{id}:state` — in-memory canonical Delta + version, for fast OT transform without a Postgres round-trip per keystroke
- `doc:{id}:presence` — set of currently connected user IDs (Phase 4)

These are caches/working state, not source of truth — Postgres `Document.content`/`version` is authoritative once persisted.

## Open decisions
- Should `Document.content` store the full Delta JSON or a compacted/compressed form? Start with plain JSON, optimize later if needed.
- Index strategy: `DocumentCollaborator(userId)` for "list my documents" query — add when `DocumentRepository` is implemented.
