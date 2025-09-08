# Repository Guidelines

## Project Structure & Modules
- Root: project meta (`docs/`, `releases/`, `.gitignore`).
- Main module: `fluid-accessibility-linter/` (JetBrains/PhpStorm plugin).
  - Source: `src/main/java/com/typo3/fluid/linter/...`
  - Resources: `src/main/resources/META-INF/plugin.xml`, inspection descriptions.
  - Tests: `src/test/java/...` with fixtures in `src/test/resources/testData/`.
  - Dev HTML fixtures: `test-templates/` for manual verification.

## Build, Test, and Run
- `./gradlew buildPlugin`: Builds the plugin ZIP in `build/distributions/`.
- `./gradlew test`: Runs unit tests (JUnit) with verbose output.
- `./gradlew runIde`: Launches a sandbox IDE to manually test inspections; open files in `test-templates/`.
- `./gradlew clean`: Cleans build outputs.

Targets: Java 17, IntelliJ Platform 2023.3 (see `build.gradle`). Do not use `plugin-universal.xml` for releases; the plugin registers against HTML via `plugin.xml`.

## Coding Style & Naming
- Java: 4‑space indent, UTF‑8, no trailing whitespace.
- Packages: lowercase (`com.typo3.fluid.linter`).
- Classes: PascalCase; methods/fields: camelCase; constants: UPPER_SNAKE.
- Inspections: place in `inspections/`; quick fixes near their inspection.
- Strategies/rules: `strategy/implementations/...` with clear, single‑purpose classes.
- Keep public APIs minimal; prefer package‑private visibility where possible.

## Testing Guidelines
- Framework: JUnit 4 (vintage engine). Name tests `*Test.java` mirroring source packages.
- Add focused tests for each new inspection/strategy. Use fixtures under `src/test/resources/testData/`.
- Run: `./gradlew test` for CI; use `./gradlew runIde` to validate highlights and quick fixes on `test-templates/`.

## Commit & Pull Requests
- Commits: short imperative subject (e.g., "Fix: avoid duplicate list warnings"). Group related changes; keep noise low.
- PRs must include:
  - Summary of changes and rationale.
  - Linked issue (e.g., `Closes #123`).
  - Before/after screenshots or code samples for inspections/quick fixes.
  - Notes on tests added/updated and any settings changes.

## Security & Configuration Tips
- Respect `sinceBuild`/`untilBuild` in `build.gradle` when adding platform APIs.
- Avoid experimental file‑type registrations in release builds; keep to `plugin.xml`.
- Do not ship private data in test fixtures or profiles; scrub `a11y-profile.json` variants.

### Notes for the implementer (GPT‑5 High)

- Be strict about **reverse‑order search** and **first hit wins** for path arrays.
- Prefer **PSI parsing** for instant editor feedback; prefer **indexes** for cross‑project queries.
- Always compute on read actions; keep UI updates on EDT; throttle via MergingUpdateQueue.
- Build clear **diff objects** (old→new resolution) to drive accurate UI changes without full redraws.
