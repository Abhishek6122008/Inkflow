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

## Real-time sync — implemented design (Phase 3)

`com.inkflow.ot` is a line-for-line Java port of `dart_quill_delta` (the OT engine `flutter_quill` itself uses), so server-side `transform`/`compose` results are bit-for-bit identical to what the Flutter client would produce — both sides converge on the same document state without drift.

- **Canonical state**: `DocumentSessionRegistry` holds one in-memory `DocumentSession` (current `Delta` content + `version` + a bounded 200-entry op history) per open document, created lazily on first edit via a `ConcurrentHashMap`.
- **Conflict resolution / versioning**: client sends `{op, baseVersion}` to `/app/doc/{id}/edit`. Server transforms `op` against every history entry newer than `baseVersion` (in order), composes the result onto the canonical content, increments `version`, and broadcasts the transformed op + new version to `/topic/doc/{id}`.
- **Echo strategy**: the server broadcasts to *all* subscribers including the original sender — there is no client-side optimistic-apply skip. The client applies whatever comes back over the topic for every author except itself (it matches on `authorId` to recognize the echo of its own op and only consumes the version number from it, since flutter_quill already applied the local edit synchronously when the user typed).
- **Persistence cadence**: every applied op is persisted to Postgres immediately (`DocumentSessionRegistry.persist`) — simplest correct option at demo scale. A debounced/snapshot cadence can replace this later without touching the OT logic itself.
- **Multi-instance backend**: explicitly out of scope — `DocumentSessionRegistry`'s state is in-memory and per-process. Running >1 backend instance requires moving this to Redis-backed shared state first.
- **Auth**: STOMP CONNECT frames carry the same JWT bearer token as REST, validated in `StompAuthChannelInterceptor` (chosen over WebSocket-handshake-header auth since handshake headers aren't reliable across all SockJS fallback transports).
- **Permission enforcement**: `DocumentService.resolveEditorUserId` (OWNER/EDITOR only) is reused by the WS controller, so the VIEWER-blocked-from-editing rule is defined in exactly one place, shared with the REST edit path.

## JDK / build note

Backend currently targets Java 21 to match the JDK at `A:\Android\jbr`. `pom.xml`'s `java.version` must always match whatever JDK `JAVA_HOME` resolves to — mismatches fail `mvn compile` with "release version not supported."
