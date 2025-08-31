# Repository Guidelines

## Project Structure & Module Organization
- Root: project metadata and docs. Main code in `fluid-accessibility-linter/`.
- Source: `fluid-accessibility-linter/src/main/java/` (Java 17), resources in `src/main/resources/` (`META-INF/plugin.xml`).
- Tests: `fluid-accessibility-linter/src/test/java/` (JUnit 4), sample HTML in `src/test/resources/` and `test-templates/`.
- Build outputs: `fluid-accessibility-linter/build/` and plugin ZIP in `build/distributions/`.

## Build, Test, and Development Commands
- `cd fluid-accessibility-linter`
- `./gradlew build`: Compile and run tests.
- `./gradlew test`: Run unit tests with logs.
- `./gradlew runIde`: Launch a sandbox IDE to try the plugin.
- `./gradlew buildPlugin`: Produce installable ZIP at `build/distributions/`.

## Coding Style & Naming Conventions
- Language: Java 17 (`sourceCompatibility/targetCompatibility` in Gradle).
- Indentation: 4 spaces; keep lines readable (<120 cols).
- Names: Classes `PascalCase`; methods/fields `camelCase`; constants `UPPER_SNAKE_CASE`.
- Packages: `com.typo3.fluid.linter...` by feature (e.g., `inspections`, `fixes`, `parser`).
- Formatting: Use IntelliJ’s Java formatter; no unchecked API usage beyond configured IntelliJ version.

## Testing Guidelines
- Framework: JUnit 4 (`testImplementation 'junit:junit:4.13.2'`).
- Location: Mirror source packages under `src/test/java`.
- Naming: `<ClassName>Test` and test methods `shouldDoX_whenY`.
- Run: `./gradlew test`. For manual checks, `./gradlew runIde` and open `test-templates/*.html`.

## Commit & Pull Request Guidelines
- Commits: Imperative, concise subject (e.g., "Add TabPanel inspection"), include scope when helpful.
- PRs: Clear description, link issues (`Fixes #123`), screenshots/GIFs for editor highlights, test updates, and short rationale.
- CI/build must pass; ensure plugin still loads in sandbox via `runIde`.

## Security & Configuration Tips
- Use JDK 17; run via Gradle wrapper.
- Respect `intellij` version and `sinceBuild/untilBuild` in `build.gradle`/`plugin.xml` when adding APIs.

## Architecture Overview (brief)
- Inspections live in `com.typo3.fluid.linter.inspections` with quick-fixes; validation strategies in `com.typo3.fluid.linter.strategy`.
- PSI helpers under `parser/`; shared utilities under `utils/`.
- Plugin registration in `src/main/resources/META-INF/plugin.xml`.

## Release Process
- Bump versions: update plugin version in `build.gradle` (Gradle IntelliJ plugin block) and, if needed, `META-INF/plugin.xml` (since/untilBuild).
- Verify locally: `./gradlew clean build` and `./gradlew runIde` to sanity‑check inspections and quick‑fixes.
- Build artifact: `./gradlew buildPlugin` → ZIP at `build/distributions/*.zip`.
- Tag and publish: create a Git tag (e.g., `vX.Y.Z`) and attach the ZIP to the release notes or upload to the IntelliJ Marketplace (requires proper plugin metadata and changelog).

## Troubleshooting
- Gradle issues: run `./gradlew --no-daemon clean build --stacktrace`. If caches are corrupt, delete `fluid-accessibility-linter/.gradle` and `build/`.
- IntelliJ sandbox: if `runIde` fails, ensure JDK 17 is selected and platform/version in `build.gradle` matches `plugin.xml`.
- Stale indexes during tests: re-run `./gradlew clean test`. If persistent, remove `build/` and retry.
- Missing ZIP: ensure you executed `./gradlew buildPlugin` inside `fluid-accessibility-linter/` and check `build/distributions/`.
