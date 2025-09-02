# A11y Auto‑Fix Plan (Remaining Only)

This file now tracks only the inspections still pending complete auto‑fix coverage. Completed
inspections (Accordion, MissingFormLabel, LiveRegion, TableAccessibility) have been removed.
Each item lists: trigger, change, constraints/safety, and tests to add.

Remaining as of now (updated Aug 31, 2025)
- AriaRoleInspection (advanced aria‑label/labelledby cleanups) — PARTIAL
- HeadingHierarchyInspection (level normalization, role=heading) — PARTIAL
- SkipLinksInspection (optional CSS hint) — PENDING
- ModalDialogInspection (labelling + focus fallback) — PARTIAL
- ListSemanticInspection (list structure fixes) — DONE (ready to remove from “remaining”)

## AriaRoleInspection (advanced)
- Status: PARTIAL
  - Completed auto-fixes:
    - Remove redundant role word from aria-label. Tests: AriaRoleAriaLabelQuickFixTest.testShouldRemoveRedundantRoleWord_fromAriaLabel
    - Remove unnecessary aria-label that duplicates visible text. Tests: AriaRoleAriaLabelQuickFixTest.testShouldRemoveUnnecessaryAriaLabel_whenDuplicatesVisibleText
    - Shorten overly long/instructional aria-label and add aria-describedby. Tests: AriaRoleAriaLabelQuickFixTest.testShouldShortenAriaLabel_andAddDescribedby
    - Validation added: aria-labelledby references non-existent IDs now flagged. Tests: AriaRoleAriaLabelAdvancedTest.testShouldWarn_whenAriaLabelledbyReferencesMissingId
  - Still to do:
    - Fix: aria-label begins with visible text
      - Trigger: Visible text and aria-label conflict.
      - Change: prepend visible text to aria-label when safe.
      - Constraints: conservative; skip when ambiguity.
      - Tests: AriaRoleInspectionQuickFixTest.shouldImproveAriaLabelOrdering()
    - Fix: Create missing IDs for aria-labelledby
      - Trigger: aria-labelledby points to non‑existent id.
      - Change: quick-fix to create stub element/id or adjust attribute.
      - Tests: AriaRoleInspectionQuickFixTest.shouldOfferCreateMissingId()

## HeadingHierarchyInspection
- Status: PARTIAL
  - Detection and messaging implemented for multiple H1s, skipped levels (context-aware), empty headings, etc. Basic level change quick-fix exists.
  - Still to do:
    - Fix: Reduce heading jump > 1 level
      - Trigger: `<h2>` followed by `<h5>` without intermediates.
      - Change: normalize to at most +1 (e.g., `h5 → h3`).
      - Tests: HeadingHierarchyInspectionQuickFixTest.shouldReduceLargeHeadingJumps()
    - Fix: Promote orphan subheading
      - Trigger: First heading is `h3+` in a region.
      - Change: raise to nearest valid top level.
      - Tests: HeadingHierarchyInspectionQuickFixTest.shouldPromoteOrphanSubheading()
    - Note: role=heading without aria-level is already covered by AriaRoleInspection (adds `aria-level=2`). No additional work required here.

## SkipLinksInspection
- Status: PENDING
  - Fix: Insert skip link at top of body
    - Trigger: Document lacks skip link.
    - Change: insert `<a class="skip-link" href="#main">Skip to content</a>` after `<body>`.
    - Constraints: avoid duplicates.
    - Tests: SkipLinksInspectionQuickFixTest.shouldInsertSkipLink()
  - Fix: Ensure main target exists
    - Trigger: No `<main>`/`id="main"` present.
    - Change: add `id="main"` to `<main>` or insert `<main id="main">`.
    - Tests: SkipLinksInspectionQuickFixTest.shouldEnsureMainTarget()
  - Fix: Provide focus-visible hint CSS (comment)
    - Trigger: No skip-link styles.
    - Change: insert commented style snippet as hint.
    - Tests: SkipLinksInspectionQuickFixTest.shouldSuggestCssComment()

## ModalDialogInspection
- Status: PARTIAL
  - Completed auto-fixes:
    - Add dialog semantics: `role="dialog"` + `aria-modal="true"` (and normalize value).
    - Focus fallback: add `tabindex="-1"` or programmatic focusability.
    - Fix positive `tabindex` to `-1`.
  - Still to do:
    - Ensure accessible name via aria-labelledby
      - Trigger: Dialog lacks label.
      - Change: insert/find heading with `id="dlg-title-<n>"`; set `aria-labelledby`.
      - Tests: ModalDialogInspectionQuickFixTest.shouldAddAriaLabelledby()
    - Wire aria-describedby (optional)
      - Trigger: Descriptive text present.
      - Change: generate id and set `aria-describedby`.
      - Tests: ModalDialogInspectionQuickFixTest.shouldWireAriaDescribedby()

## ListSemanticInspection
- Status: DONE
  - Implemented fixes (with quick-fixes and tests in place):
    - Unwrap single-item list (plain or paragraph).
    - Wrap stray content in `<li>`.
    - Remove empty `<li>`.
    - Remove redundant `role="list"` on `<ul>/<ol>`.
  - Action: Move this section to “Completed” (it no longer belongs in “Remaining Only”).

---

## Test Strategy (shared)
- Use existing `LightJavaCodeInsightFixtureTestCase` patterns; assert highlight → quick‑fix availability → result after apply.
- Naming: `<Inspection>QuickFixTest.shouldDoX_whenY()` under `src/test/java/.../inspections/`.
- Idempotence: re‑applying a fix should be a no‑op.

## Safety & IDs
- Stable id prefixes: `acc-panel-`, `acc-trigger-`, `fld-`, `dlg-title-`, `th-`.
- Never overwrite existing valid attributes; prefer adding/normalizing/removing redundant only.
- Keep transformations localized; avoid large structural rewrites unless explicitly guarded by heuristics.
