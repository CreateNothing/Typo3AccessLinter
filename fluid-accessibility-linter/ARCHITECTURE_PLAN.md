# Fluid Accessibility Linter — Architecture & Scalability Plan

This document outlines a future‑proof, scalable architecture for the Fluid Accessibility Linter
targeting TYPO3 Fluid compatibility in PhpStorm.

## 1) Current State (Summary)

- Universal core: Rule engine + strategies + fix registry (`rules/*`, `strategy/*`, `fixes/*`) and
  `UniversalAccessibilityInspection` exist and work alongside older inspections.
- Legacy layer: Multiple monolithic inspections remain registered in `plugin.xml` (regex-heavy,
  large classes, overlapping logic with strategies).
- Fluid detection: Heuristics run on HTML files (no custom filetype in main plugin.xml — good).
- Alternate manifest: `plugin-universal.xml` registers a single inspection and a custom Fluid
  filetype for `.html` (unsafe; conflicts with HTML and should not be used for release builds).
- Utilities: PSI helpers (`PsiElementParser`), some strategy implementations, and early fix
  strategies (several stubs or duplicated quick fixes in inspections).

## 2) Key Gaps to Address

- Duplication: Rule engine and legacy inspections coexist; drift risk and higher maintenance cost.
- Fragile parsing: Regex over full file text in several inspections instead of PSI-first visitors.
- Extensibility: Rule/fix providers are hard-wired; no IntelliJ extension points for external
  contributions or modular rollout.
- Configuration: No user-facing rule management (enable/disable, severity override, rule config).
- Quick fixes: Strategy-based fixes are incomplete; PSI mutation helpers are not centralized.
- Testing: No IntelliJ inspection tests; validation is manual via `test-templates/`.

## 3) Goals

- Maintainability: Single universal entry point; modular strategies; minimal duplication.
- Accuracy: Prefer PSI traversal over regex; reliable Fluid ViewHelper handling.
- Extensibility: Pluggable rule and fix providers via extension points; clear rule metadata.
- Performance: Incremental PSI visitors, targeted scopes, caching, and prioritized strategies.
- UX/Config: Project-level settings for rules, severities, and import/export of rule profiles.

## 4) Architecture Improvements

### 4.1 Single Universal Entry Point

- Default to `UniversalAccessibilityInspection` to execute all enabled rules from the rule engine.
- Keep legacy inspections temporarily behind a toggle/profile for transition; phase them out later.

### 4.2 PSI‑First Strategies

- Port regex-based logic to `ValidationStrategy` implementations using PSI visitors
  (`PsiRecursiveElementVisitor`, `PsiElementParser` helpers). Use regex only for targeted edge
  cases (e.g., comments/scripts) when PSI is insufficient.

### 4.3 IntelliJ Extension Points for Providers

- Define extension points: `ruleProvider` (for `RuleProvider`) and `fixStrategy` (for `FixStrategy`).
- Load providers via `ExtensionPointName` in `RuleEngine` instead of hardcoding defaults. Keep
  `DefaultRuleProvider` and `FluidRuleProvider` as built-in providers registered through EPs.

### 4.4 Project‑Level Configuration

- Implement a `PersistentStateComponent` to store per-rule settings: enabled, severity, and rule
  configuration (key/value). Provide a simple Settings UI to manage rules and import/export YAML/JSON
  profiles (e.g., WCAG 2.1 AA defaults, team presets).

### 4.5 Fluid‑Aware Utilities

- Add `FluidPsiUtils` to identify Fluid namespaces and distinguish control‑flow versus output
  ViewHelpers. Provide utilities to logically “unwrap” control‑flow wrappers when validating direct
  child constraints (e.g., `<ul>` children via `<f:if>` wrappers).

### 4.6 Fix Strategy Layer

- Centralize PSI mutations in helper methods used by `FixStrategy`:
  - Add/change/remove attribute
  - Wrap/unwrap element
  - Insert sibling/child elements
  - Use `WriteCommandAction` to modify PSI safely
- Route quick fixes through `FixRegistry` with typed `FixContext` instead of embedding fixes inside
  inspections. Keep inspection classes thin.

### 4.7 Packaging Sanity

- Do not register a custom file type for `.html`. Keep inspections bound to `language="HTML"` in
  the main `plugin.xml`. Delete or restrict `plugin-universal.xml` usage to internal testing only.

## 5) Performance & Reliability

- Incremental inspection: Build PSI visitors and avoid scanning the whole file as plain text.
- Caching: Use `PsiModificationTracker` to invalidate per-file caches of computed findings.
- Prioritization: Use `ValidationStrategy#getPriority()` to run lightweight, high-signal checks first.
- Targeted scopes: Traverse only relevant PSI branches (e.g., only `<ul|ol|dl>` for list rules).

## 6) Migration Plan (Phased)

### Phase 1 — Activate Universal Path

- Register `UniversalAccessibilityInspection` in main `plugin.xml` as enabled-by-default.
- Keep legacy inspections selectable behind a “Use legacy inspections” toggle/profile.
- Remove `FluidFileType` registration for `.html` from any manifests intended for release.

### Phase 2 — Port Inspections to Strategies

- Migrate legacy inspections to PSI-first `ValidationStrategy` implementations one-by-one.
- Wire strategies in `DefaultRuleProvider`/`FluidRuleProvider` and remove duplicate logic.
- Implement quick fixes through `FixRegistry` with PSI mutation helpers.

### Phase 3 — Extensibility & Settings

- Introduce extension points for rule/fix providers and load them dynamically.
- Add `PersistentStateComponent` + Settings UI for rule enablement, severity overrides,
  and per-rule configs; support import/export of rule profiles.

### Phase 4 — Cleanup & Compatibility

- Remove legacy inspections from `plugin.xml` once strategy parity is reached and tested.
- Keep `sinceBuild`/`untilBuild` aligned with supported IntelliJ versions; avoid newer APIs unless
  gated with version checks.

### Phase 5 — Tests & Documentation

- Add IntelliJ inspection tests (CodeInsightFixture) for each rule and integration tests using
  `test-templates/*.html`.
- Document rule IDs, categories, configuration options, and quick fix behaviors.

## 7) Implementation Checklist

- Register universal inspection in `plugin.xml`; add a user toggle for legacy vs universal.
- Introduce extension points and refactor `RuleEngine` to load providers via EPs.
- Implement PSI mutation helpers and refactor `FixRegistry` strategies to use them.
- Port representative inspections (e.g., Missing Alt, List Structure, ARIA Role) to PSI strategies;
  validate with tests and samples.
- Add `PersistentStateComponent` + Settings UI; support severity overrides and profiles.
- Remove legacy inspections after parity; delete `plugin-universal.xml`’s `.html` file type mapping.
- Add inspection tests and update developer documentation.

## 8) Scalability & Future‑Proofing

- Rule metadata: Stable IDs, categories, tags, and WCAG mapping for profile portability.
- Multiple providers: Support additional Fluid ecosystems (e.g., VHS `v:*`, Flux) via EPs.
- Backward compatibility: Maintain `sinceBuild/untilBuild` compatibility; gate newer APIs.
- Observability: Optional debug logs and a “rule stats” panel to spot slow strategies.

## 9) Open Questions

- Integrate with an existing TYPO3 Fluid plugin for richer PSI of ViewHelpers or continue with
  lightweight helpers only?
- Which Fluid namespaces (v:, flux:, etc.) should be recognized out of the box?
- Prefer storing rule profiles in project VCS (YAML/JSON) or only IDE settings?

## 10) Suggested Package Structure (Refined)

```
com.typo3.fluid.linter
  ├── inspections/                     # Thin inspection entry points only
  ├── parser/                          # PSI helpers (HTML/Fluid utils)
  ├── rules/                           # Rule engine + providers + domain model
  ├── strategy/                        # ValidationStrategy + implementations
  ├── fixes/                           # FixStrategy + PSI mutation helpers
  └── utils/                           # A11y utilities (WCAG, ARIA, etc.)
```

— End of plan —

