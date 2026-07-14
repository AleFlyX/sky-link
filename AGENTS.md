# Repository Guidelines

## Project Structure & Module Organization

This repository is split into three main areas:

- `docs/`: product and data-design docs, including `spec.md`, `model.md`, `api.md`, and `backend/sql.md`.
- `frontend/sky-link-frontend/`: Vue 3 + Vite client app. Main code lives in `src/`, with tests in `src/__tests__/`.
- `backend/land/`: Spring Boot backend. Java sources live under `src/main/java/com/skylink/land/`, configuration in `src/main/resources/`, and tests in `src/test/java/`.

Entity classes are grouped by domain such as `entity/chat`, `entity/file`, `entity/task`, and composite IDs under `entity/**/id`.

## Build, Test, and Development Commands

Frontend, from `frontend/sky-link-frontend/`:

- `npm install`: install dependencies.
- `npm run dev`: start the Vite dev server.
- `npm run build`: create a production build.
<!-- - `npm run test:unit`: run Vitest unit tests. -->
- `npm run lint`: run `oxlint` and `eslint` with auto-fix.
- `npm run format`: format `src/` with Prettier.

Backend, from `backend/land/`:

- `./mvnw test` or `mvnw.cmd test`: run Spring Boot tests.
- `./mvnw spring-boot:run` or `mvnw.cmd spring-boot:run`: start the backend locally.
- `./mvnw package`: build the runnable JAR.

## Coding Style & Naming Conventions

Use 2 spaces in frontend JavaScript/Vue files and 4 spaces in Java files. Prefer `PascalCase` for Vue components and Java classes, `camelCase` for variables and methods, and `UPPER_SNAKE_CASE` for constants. Keep backend packages domain-oriented (`identity`, `chat`, `file`, `audit`). Run `npm run lint` before committing frontend changes.

## Testing Guidelines

Frontend tests use Vitest with files named `*.spec.js` under `src/__tests__/`. Backend tests use Spring Boot Test under `src/test/java/`; follow the `*Tests.java` naming pattern. Add or update tests whenever behavior, validation, or entity mappings change.

## Commit & Pull Request Guidelines

Follow the existing commit style seen in history: concise prefixes such as `docs:`, `init:`, and `feat:`. Example: `docs: align model and sql with spec`. Keep each commit focused on one concern.

Do not combine all task changes into one commit. Split commits by independently reviewable concern, such as security configuration, bootstrap data, schema initialization, and product documentation. Stage files with explicit paths, inspect `git diff --cached` before every commit, and avoid `git add .` or `git add -A`. When practical, each commit should build and pass the tests relevant to that concern.

PRs should include:

- a short summary of what changed,
- affected areas (`docs`, `frontend`, `backend`),
- screenshots for UI changes,
- notes on tests run or why tests were skipped.

## Documentation & Data Model Notes

When updating entities or SQL, keep `docs/spec-current.md`, `docs/model.md`, and `docs/backend/sql.md` aligned. Preserve `docs/spec.md` as the original full-scope vision unless the task explicitly changes that historical baseline. If a new feature adds a table or field, update both the current model explanation and SQL schema in the same change.
