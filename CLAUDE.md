# Inkflow

Collaborative rich-text editor. Multiple users edit the same document in real time, changes sync via operational transform (OT) over WebSocket, and documents persist in Postgres.

## Stack

**Client** — Flutter (`lib/`)
- `flutter_riverpod` — state management
- `dio` + `retrofit` — REST API client
- `stomp_dart_client` — WebSocket/STOMP for real-time sync
- `flutter_quill` (`9.2.3`, pinned — newer versions need Dart `^3.12`, this project is on `3.10.7`) — rich text editor widget
- `go_router` — navigation
- `flutter_secure_storage` / `shared_preferences` — token/local storage
- `nes_ui` — app-wide UI theme (retro NES-styled widgets, `flutterNesTheme()`)

**Backend** — Spring Boot 3.5.14, Java 21 (`inkflow-backend/`)
- Postgres (JPA/Hibernate, `ddl-auto=update` for now)
- Redis — likely session/presence/cache for collaborative state
- JWT auth (`jjwt`)
- WebSocket (STOMP) for real-time document sync
- Package layout under `com.inkflow`: `config`, `controller`, `dto`, `entity`, `enums`, `exception`, `repository`, `security`, `service`, `websocket`, `ot` (operational transform logic lives here)

## Repo layout

```
lib/                      Flutter app source
inkflow-backend/          Spring Boot backend
  src/main/java/com/inkflow/<package>/
  src/main/resources/application.properties
docs/                     Planning docs (architecture, roadmap, API, data model)
```

## Conventions / constraints

- Java version must match the installed JDK at `A:\Android\jbr` (currently 21 — do not bump `java.version` in `pom.xml` without confirming the JDK supports it; see `docs/architecture.md`).
- `flutter_quill` is pinned below 11.5.1 due to Dart SDK constraint — don't upgrade without bumping the Flutter/Dart SDK first.
- Backend packages are currently empty skeletons (`.gitkeep` only) — see `docs/roadmap.md` for build order.
- Use `docs/api-contract.md` as the source of truth for endpoint shapes when implementing controllers or the Flutter API client — keep both sides in sync when changing it.
- Use `docs/data-model.md` as the source of truth for entities — update it alongside JPA entity changes.

## Running things

Backend:
```
cd inkflow-backend
$env:JAVA_HOME = "A:\Android\jbr"; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\mvnw clean install -DskipTests
```

Flutter:
```
flutter pub get
flutter run
```
