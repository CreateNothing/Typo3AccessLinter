# A11y Auto‑Fix Spec (HTML/Fluid)

This document enumerates quick‑fix transformations we apply to Fluid/HTML, with triggers, before/after examples, constraints, and tests to add. We will write tests first for each item before expanding scope.

## NavigationLandmarkInspection
- Fix: Add aria-label to <nav>
  - Trigger: Multiple <nav> elements and the current <nav> lacks aria-label/aria-labelledby.
  - Change: `<nav ...>` → `<nav ... aria-label="Navigation">`
  - Constraints: Skip if already labeled.
  - Tests: NavigationLandmarkInspectionTest.shouldAddAriaLabel_whenMultipleNavs()

- Fix: Remove redundant role="navigation"
  - Trigger: Element has role="navigation" where semantic <nav> is preferable or redundant.
  - Change: remove role attribute.
  - Tests: NavigationLandmarkInspectionTest.shouldRemoveRedundantRole_onNavRole()

- Fix: Insert <main>
  - Trigger: No <main> present (documents >500 chars).
  - Change: Insert `<main role="main">` after <body>.
  - Tests: NavigationLandmarkInspectionTest.shouldInsertMain_whenMissing()

## TabPanelInspection
- Fix: Add aria-selected (tab)
  - Trigger: role="tab" without aria-selected.
  - Change: add `aria-selected="false"`.
  - Tests: TabPanelInspectionQuickFixTest.shouldAddAriaSelected()

- Fix: Normalize aria-selected
  - Trigger: aria-selected has invalid value.
  - Change: set to `false` (tests will also cover setting exactly one true via consistency fix).
  - Tests: TabPanelInspectionQuickFixTest.shouldNormalizeAriaSelected()

- Fix: Add aria-controls (tab)
  - Trigger: role="tab" without aria-controls.
  - Change: add `aria-controls="panel-<id>"` (generated).
  - Tests: TabPanelInspectionQuickFixTest.shouldAddAriaControls()

- Fix: Add id (tabpanel)
  - Trigger: role="tabpanel" lacks id.
  - Change: add `id="panel-<id>"`.
  - Tests: TabPanelInspectionQuickFixTest.shouldAddPanelId()

- Fix: Add aria-labelledby (tabpanel)
  - Trigger: role="tabpanel" lacks aria-labelledby.
  - Change: set to an existing/generates tab id.
  - Tests: TabPanelInspectionQuickFixTest.shouldLinkPanelToTab()

- Fix: Add aria-label to tablist
  - Trigger: role="tablist" lacks aria-label/labelledby.
  - Change: add `aria-label="Tabs"`.
  - Tests: TabPanelInspectionQuickFixTest.shouldLabelTablist()

- Fix: Normalize aria-orientation
  - Trigger: Invalid/unsupported value.
  - Change: set to `horizontal`.
  - Tests: TabPanelInspectionQuickFixTest.shouldNormalizeOrientation()

- Fix: Tabindex (generic/selected/inactive)
  - Trigger: Missing or incorrect per selection state.
  - Change: add/fix tabindex to 0 for selected, -1 for inactive.
  - Tests: TabPanelInspectionQuickFixTest.shouldFixTabindexSelected(), shouldFixTabindexInactive()

- Fix: Focusable tabpanel
  - Trigger: role="tabpanel" not focusable.
  - Change: add `tabindex="0"`.
  - Tests: TabPanelInspectionQuickFixTest.shouldAddPanelTabindex()

- Fix: Selection consistency
  - Trigger: multiple `aria-selected="true"` or none.
  - Change: set one true, others false; keep DOM order preference.
  - Tests: TabPanelInspectionQuickFixTest.shouldEnforceSingleSelection()

- Fix: Show/Hide tabpanel
  - Trigger: mismatch between selection and visibility.
  - Change: show → remove `hidden` and set `aria-hidden="false"`; hide → add `hidden` and set `aria-hidden="true"`.
  - Tests: TabPanelInspectionQuickFixTest.shouldSyncPanelVisibility()

## ListSemanticInspection
- Fix: Convert single-item list → plain content
  - Trigger: <ul>/<ol> with exactly one <li>.
  - Change: unwrap to inner HTML.
  - Tests: ListSemanticInspectionQuickFixTest.singleItem_unwrap()

- Fix: Convert single-item list → <p>
  - Trigger: same; only when inner is inline.
  - Change: wrap inner in <p>…</p>.
  - Tests: ListSemanticInspectionQuickFixTest.singleItem_toParagraph()

- Fix: Flatten deep nesting (merge to parent)
  - Trigger: nesting depth > 4.
  - Change: lift inner <li>, merge nested list classes/ARIA to parent.
  - Tests: ListSemanticInspectionQuickFixTest.flatten_mergeParent_keepsAttrs()

- Fix: Flatten deep nesting (move classes to items)
  - Trigger: same; alternative.
  - Change: lift inner <li>, push merged classes to first-level items; keep parent minimal.
  - Tests: ListSemanticInspectionQuickFixTest.flatten_moveClassesToItems()

- Fix: Wrap stray content in <li>
  - Trigger: direct child that isn’t <li>.
  - Change: wrap offending element in <li>.
  - Tests: ListSemanticInspectionQuickFixTest.wrapStrayContent()

- Fix: Remove empty <li>
  - Trigger: `<li></li>` (optionally with <br> or whitespace).
  - Change: remove the item.
  - Tests: ListSemanticInspectionQuickFixTest.removeEmptyItem()

- Fix: Remove role="list" on <ul>/<ol>
  - Trigger: role="list" present.
  - Change: remove role attribute.
  - Tests: ListSemanticInspectionQuickFixTest.removeRedundantRole()

- Fix: Change <ul>↔<ol>
  - Trigger: content type mismatch (steps/ranking vs features/options).
  - Change: swap tag names.
  - Tests: ListSemanticInspectionQuickFixTest.swapListType_ulToOl(), _olToUl()

- Fix: Remove leading bullet glyphs inside <li>
  - Trigger: <li> starts with • · – — - + *
  - Change: strip leading glyphs.
  - Tests: ListSemanticInspectionQuickFixTest.removeBulletGlyph()

- Fix: Number-only item in <ul>
  - Trigger: item text is only digits.
  - Change: convert surrounding list to <ol>.
  - Tests: ListSemanticInspectionQuickFixTest.numberOnlyItem_ulToOl()

- Fix: Trailing colon without nested list
  - Trigger: item text ends with ':' and no nested list.
  - Change: remove trailing colon.
  - Tests: ListSemanticInspectionQuickFixTest.removeTrailingColon()

- Fix: Add aria-label to first interactive in <li>
  - Trigger: input/button/select without accessible name.
  - Change: add `aria-label="Action"` (placeholder).
  - Tests: ListSemanticInspectionQuickFixTest.addAriaLabelToInteractive()

## Planned (next)
- AccordionAccessibilityInspection: add aria-expanded, role=button, tabindex, aria-controls; keyboard/focus hints.
- MissingFormLabelInspection: add <legend> to fieldset; align required vs aria-required; convert placeholder labeling.
- LiveRegionInspection: normalize aria-live/atomic/relevant; add role=alert/status; aria-busy management.
- TableAccessibilityInspection: header cells conversion, caption, thead/tbody, headers/scope relationships.
- AriaRoleInspection: reduce multiple roles, add required ARIA props, remove conflicting/presentation roles.
- SkipLinksInspection: insert skip link at top; add focus-visible CSS suggestion.

## Test Strategy
- Use existing LightJavaCodeInsightFixtureTestCase tests; assert highlight + quick-fix text presence, then apply quick-fix and assert resulting HTML.
- Prefer small, focused fixtures with minimal markup; include Fluid tags where relevant (e.g., <f:link.*>, <f:image>). 
- Naming: <Inspection>QuickFixTest.shouldDoX_whenY(). Place under `src/test/java/.../inspections/`.

## Rollback/Safety
- All transformations are best-effort string/PSI edits; where ids are generated, prefer stable prefixes (`tab-`, `panel-`).
- Avoid editing when conflicting attributes are already present (e.g., skip if aria-* exists). 
- Prefer no-ops over risky rewrites; tests must verify idempotence (applying the same fix twice is a no-op).
