<div align="center">

# 📝 Inkflow

**A collaborative rich-text editor with real-time, conflict-free editing.**

Multiple people, one document, zero merge conflicts — edits sync live via operational transform (OT) over WebSocket.

[![Flutter](https://img.shields.io/badge/Flutter-3.10-02569B?logo=flutter&logoColor=white)](https://flutter.dev)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## ✨ What is Inkflow?

Inkflow is a **Google Docs–style collaborative editor**: open the same document from two devices and watch edits appear live on both, without stepping on each other. Under the hood, every keystroke is a Quill Delta op that the backend transforms against concurrent edits before broadcasting it back out — that's the "OT" in operational transform.

> 🚧 **Status: early scaffolding.** Auth, document CRUD, and real-time sync are being built in that order. See [docs/roadmap.md](docs/roadmap.md) for exactly where things stand.

## 🧱 Stack

<table>
<tr>
<td valign="top" width="50%">

### Client — Flutter (`lib/`)

| Package | Purpose |
|---|---|
| `flutter_riverpod` | State management |
| `dio` + `retrofit` | REST API client |
| `stomp_dart_client` | WebSocket/STOMP, real-time sync |
| `flutter_quill` `9.2.3` | Rich text editor (Delta model) |
| `nes_ui` | App-wide retro UI theme |
| `go_router` | Navigation |
| `flutter_secure_storage` | JWT/token storage |

</td>
<td valign="top" width="50%">

### Backend — Spring Boot (`inkflow-backend/`)

| Component | Purpose |
|---|---|
| Spring Boot 3.5.14 / Java 21 | App framework |
| PostgreSQL + JPA/Hibernate | Document & user storage |
| Redis | Presence, OT session cache |
| `jjwt` | Stateless JWT auth |
| WebSocket + STOMP | Real-time op broadcast |
| `com.inkflow.ot` | Operational transform engine |

</td>
</tr>
</table>

**Why this pairing?** Quill's Delta format (`insert` / `retain` / `delete`) is already OT-shaped, so the client's document model maps directly onto a server-authoritative OT engine — no translation layer needed. Full rationale in [docs/architecture.md](docs/architecture.md).

## 🗺️ Architecture at a glance

```
┌───────────────────┐        REST (Dio/Retrofit)         ┌────────────────────────┐
│    Flutter App    │ ───────────────────────────────    │    Spring Boot API     │
│      (lib/)       │ ───────────────────────────────    │  (inkflow-backend/)    │
│                   │                                    │                        │
│   flutter_quill   │     STOMP over WebSocket (/ws)     │ WebSocket + OT engine  │
│   (editor widget) │ ◀──────────────────────────────▶  │   com.inkflow.ot       │
└───────────────────┘                                    └────────────────────────┘
                                                                       │
                                                          ┌────────────┴────────────┐
                                                          ▼                         ▼
                                                     PostgreSQL                   Redis
                                                 (documents, users,           (presence, live
                                                    doc history)              OT state cache)
```

## 📂 Project structure

```
Inkflow/
├── lib/                              Flutter app source
├── inkflow-backend/
│   └── src/main/java/com/inkflow/
│       ├── config/                   Spring configuration
│       ├── controller/               REST + WebSocket controllers
│       ├── dto/                      Request/response payloads
│       ├── entity/                   JPA entities
│       ├── enums/                    Shared enums (e.g. DocumentRole)
│       ├── exception/                Error handling
│       ├── ot/                       Operational transform engine
│       ├── repository/               Spring Data repositories
│       ├── security/                 JWT auth, Spring Security config
│       ├── service/                  Business logic
│       └── websocket/                STOMP config & handlers
├── docs/
│   ├── architecture.md               System design + open decisions
│   ├── roadmap.md                    Phased build plan
│   ├── api-contract.md               REST + STOMP endpoint contracts (draft)
│   └── data-model.md                 Entity/schema design (draft)
└── CLAUDE.md                         Conventions for AI-assisted dev
```

## 🚀 Getting started

### Prerequisites

- [Flutter SDK](https://flutter.dev) (Dart `^3.10.7`)
- JDK 21 (this project uses `A:\Android\jbr`)
- PostgreSQL running locally, with a database named `inkflow`
- Redis running locally

### 1. Backend

```powershell
cd inkflow-backend
$env:JAVA_HOME = "A:\Android\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw clean install -DskipTests
.\mvnw spring-boot:run
```

Server starts on `http://localhost:8080`. Connection settings (Postgres/Redis credentials, JWT secret) live in [inkflow-backend/src/main/resources/application.properties](inkflow-backend/src/main/resources/application.properties) — defaults assume `postgres`/`postgres` on `localhost:5432` and Redis on `localhost:6379`.

### 2. Flutter app

```powershell
flutter pub get
flutter run
```

### Troubleshooting

| Problem | Fix |
|---|---|
| `mvn compile` fails with "release version not supported" | `java.version` in `pom.xml` must match the JDK `JAVA_HOME` points to. Check with `java -version`. |
| `flutter pub get` version conflicts | Check `flutter_quill` (pinned `^9.2.3` — newer needs Dart `^3.12`) and `nes_ui` (pinned `^0.30.0` for `google_fonts ^6.x` compatibility) before bumping either. |
| Backend can't connect to Postgres/Redis | Confirm both are running locally and credentials in `application.properties` match your local setup. |

## 📖 Documentation

| Doc | Covers |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Repo conventions, constraints, and quick commands |
| [docs/architecture.md](docs/architecture.md) | System design, component rationale, open technical decisions |
| [docs/roadmap.md](docs/roadmap.md) | Phased build plan (auth → CRUD → real-time sync → polish) |
| [docs/api-contract.md](docs/api-contract.md) | Draft REST + STOMP endpoint shapes |
| [docs/data-model.md](docs/data-model.md) | Draft entities and schema design |

## 🛣️ Roadmap snapshot

- [x] **Phase 0** — Project scaffolding (backend skeleton, Flutter deps)
- [ ] **Phase 1** — Auth (register/login, JWT)
- [ ] **Phase 2** — Document CRUD
- [ ] **Phase 3** — Real-time collaborative editing (the core feature)
- [ ] **Phase 4** — Presence & polish
- [ ] **Phase 5** — Hardening (history, permissions, tests)

Full detail in [docs/roadmap.md](docs/roadmap.md).

## 📄 License

MIT
