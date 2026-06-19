# Architecture

## Overview

Inkflow is a collaborative rich-text editor: multiple clients edit the same document concurrently, and edits propagate in real time via operational transform (OT) over WebSocket. REST handles everything that isn't live-typing (auth, document list, document metadata, history). WebSocket/STOMP handles the live edit stream.

```
┌─────────────────┐         REST (Dio/Retrofit)        ┌──────────────────────┐
│   Flutter App    │ ──────────────────────────────────▶│   Spring Boot API     │
│  (lib/)           │ ◀──────────────────────────────────│  (inkflow-backend/)   │
│                    │                                     │                        │
│  flutter_quill     │   STOMP over WebSocket (/ws)        │  WebSocket + OT engine │
│  (editor widget)   │ ◀──────────────────────────────────▶│  com.inkflow.ot       │
└─────────────────┘                                     └──────────────────────┘
                                                                    │
                                                          ┌─────────┴─────────┐
                                                          │                   │
                                                     Postgres            Redis
                                                  (documents, users,   (presence,
                                                   doc history)         active-session
                                                                        OT state cache)
```

## Why each piece

- **flutter_quill**: gives us a Delta-based rich text document model. Quill Deltas are themselves an operational-transform-friendly format (insert/retain/delete ops), which is why this OT approach pairs naturally with it on the client.
- **com.inkflow.ot (backend)**: server-authoritative OT — transforms incoming ops against concurrent ops, rebroadcasts the transformed op to other clients, keeps a canonical document state. This is the core of the product; everything else is scaffolding around it.
- **stomp_dart_client / Spring WebSocket+STOMP**: pub/sub channel per document (e.g. `/topic/doc/{id}`) for live op broadcast, plus a send destination for clients to push their local ops (e.g. `/app/doc/{id}/edit`).
- **Redis**: candidate uses — presence (who's currently in a document), the current in-memory OT document state/version counter for fast transform without hitting Postgres on every keystroke, pub/sub fan-out if the backend ever scales beyond one instance.
- **Postgres**: durable storage — documents, users, and periodic snapshots/history of document content (not necessarily every single op).
- **JWT**: stateless auth for REST; the same token is used to authenticate the STOMP CONNECT frame.

## Open decisions (resolve before/while building real-time sync)

- Snapshot cadence: do we persist on every op, debounce, or snapshot every N ops / T seconds?
- Multi-instance backend: if we ever run >1 backend instance, OT state in Redis (not just in-memory) becomes mandatory for correctness. Single instance is fine for now.
- Conflict/version numbering scheme for OT (client revision tracking — needed so the server knows what to transform incoming ops against).

These should be settled and written into this doc once `com.inkflow.ot` design starts (see `roadmap.md` Phase 3).

## JDK / build note

Backend currently targets Java 21 to match the JDK at `A:\Android\jbr`. `pom.xml`'s `java.version` must always match whatever JDK `JAVA_HOME` resolves to — mismatches fail `mvn compile` with "release version not supported."
