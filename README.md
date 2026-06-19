# Inkflow

Collaborative rich-text editor. Multiple users edit the same document in real time — edits sync via operational transform (OT) over WebSocket, and documents persist in Postgres.

## Stack

**Client** — Flutter (`lib/`)
- `flutter_riverpod` — state management
- `dio` + `retrofit` — REST API client
- `stomp_dart_client` — WebSocket/STOMP for real-time sync
- `flutter_quill` (`9.2.3`, pinned) — rich text editor widget, Delta-based document model
- `nes_ui` — app-wide UI theme
- `go_router` — navigation
- `flutter_secure_storage` / `shared_preferences` — token/local storage

**Backend** — Spring Boot 3.5.14, Java 21 (`inkflow-backend/`)
- Postgres (JPA/Hibernate) — document and user storage
- Redis — presence / collaborative session state
- JWT (`jjwt`) — auth
- WebSocket (STOMP) — real-time document sync
- `com.inkflow.ot` — operational transform engine

See [CLAUDE.md](CLAUDE.md) for repo conventions and [docs/](docs/) for architecture, roadmap, API contract, and data model.

## Prerequisites

- Flutter SDK (Dart `^3.10.7`)
- JDK 21 (project currently uses `A:\Android\jbr`)
- PostgreSQL running locally, database `inkflow`
- Redis running locally

## Setup

### Backend

```powershell
cd inkflow-backend
$env:JAVA_HOME = "A:\Android\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw clean install -DskipTests
.\mvnw spring-boot:run
```

Config lives in [inkflow-backend/src/main/resources/application.properties](inkflow-backend/src/main/resources/application.properties) — update Postgres/Redis credentials there if yours differ from the defaults (`postgres`/`postgres`, `localhost:5432`/`localhost:6379`).

Server runs on `http://localhost:8080`.

### Flutter app

```powershell
flutter pub get
flutter run
```

## Project status

Early scaffolding stage — auth, document CRUD, and real-time sync are not yet implemented. See [docs/roadmap.md](docs/roadmap.md) for the build order and [docs/api-contract.md](docs/api-contract.md) / [docs/data-model.md](docs/data-model.md) for the (draft) contracts being built against.
