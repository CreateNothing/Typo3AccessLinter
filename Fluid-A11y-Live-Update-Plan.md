# Fluid A11y Plugin — **Live Updates, Adds/Deletes, and Graph Maintenance**

**Audience:** Implementation hand‑off for *GPT‑5 High* (senior engineer persona) implementing live, incremental updates in a PhpStorm/IntelliJ plugin that audits TYPO3 Fluid heading structure across templates, layouts, and partials.

**Scope of this addendum:** Extend the existing plan with concrete mechanics for **file additions, deletions, renames, and content edits**; **configuration changes** (TypoScript & site settings); **override precedence recalculation**; and **live UI refresh** for the graph and the heading outline.

---

## 1) What must stay true after any change

When **any** relevant input changes (files or config), the system must quickly produce a consistent state:

1. **Graph correctness:** Include edges and ancestry must reflect the latest filesystem + configuration.
2. **Effective implementation resolution:** For each `(Context, LogicalName, Kind)` the **currently active file** is derived by reverse‑order path search; overrides and fallbacks are correctly picked.
3. **Incremental recomputation:** Only recompute affected portions (changed files and their transitive closure to entry points).
4. **UI freshness with bounded work:** Toolwindows (Parents & Overrides, Heading Outline) update automatically; changes are debounced and never block typing.
5. **Grace under indexing:** During IDE indexing (“dumb mode”), heavy computations pause or degrade gracefully, then auto‑refresh in smart mode.

---

## 2) Change types to handle

- **File content change** (save) — Fluid file (`*.html`), TypoScript (`*.typoscript`, `*.ts`), YAML site config/settings under `config/sites/**`, plugin view config, etc.
- **File creation** — new template/layout/partial anywhere under any root path.
- **File delete**
- **File move/rename** — treat as delete + add.
- **Unsaved edits** — in-memory PSI changes before save (for live outline in the editor tab).
- **Project structure / roots changes** — new content roots, composer/vendor refresh.
- **Indexing phase transitions** — enter/exit Dumb Mode.

---

## 3) Event sources & listeners (IntelliJ Platform)

> All listeners must filter to relevant files and post work to a **MergingUpdateQueue** (200–400ms) to coalesce bursts.

- **VFS changes (add/delete/rename/move/content change)**  
  Subscribe a **BulkFileListener** via the project message bus. Inspect events for relevant extensions/paths and enqueue *GraphUpdateTasks*.

- **Unsaved in‑editor edits (pre‑save)**  
  - Add a **DocumentListener** for open Fluid files (via `EditorFactory.getInstance().eventMulticaster`), and/or a **PsiTreeChangeListener** filtered to Fluid PSI.  
  - For the **active editor tab**, compute an **ephemeral** outline directly from PSI (not the index) so users see heading/a11y hints before saving.

- **Configuration files**  
  - Watch TypoScript and site YAML files with the same VFS listener; enqueue **ContextRebuildTasks** for impacted contexts (see §6).

- **Project/root changes**  
  - Subscribe to **ProjectRootManager.TOPIC** (roots changed). Re‑scan path sets and refresh ImplementationCatalogs.

- **Indexing state**  
  - Use **DumbService** to detect dumb/smart transitions. In dumb mode, show “partial results; updating…” badges. On smart‑entered, trigger a coalesced **GraphRefreshTask**.

---

## 4) Data structures recap (minimal)

```text
ContextId         // e.g., PAGEVIEW:root, FLUIDTEMPLATE:page, EXTBASE:tx_news:list
RootPathSet       // { templates: [Path..], layouts: [Path..], partials: [Path..] } (ordered, reverse search)
LogicalName       // e.g., "Navigation/Breadcrumb"
Implementation    // physical file for a LogicalName
IncludeEdge       // fromFile → (kind, logicalName?, attributes, range, isDynamic)
Callsite          // IncludeEdge + Context + enclosing DOM snippet
EntryPoint        // (file, Context)
```

**Indexes & caches** (incremental):
- **IncludeIndex**: `file → IncludeEdges[]`
- **ReverseIncludeIndex**: `logicalName → Callsite[]`
- **ContextIndex**: `file → ContextId`
- **ImplementationCatalog**: `(ContextId, Kind, LogicalName) → [Implementation candidates ordered by priority]`
- **EffectiveResolutionCache**: `(ContextId, Kind, LogicalName) → Implementation?`
- **FlattenCache**: `(EntryPoint) → FlattenResult { headings[], sources[], completeness, dependsOnFiles[] }`

All caches are invalidated **surgically** based on change impact (below).

---

## 5) Incremental update pipeline

### 5.1 Dispatcher

All listeners push `ChangeEvents` into a **MergingUpdateQueue**. Each event includes:
- `kind`: CONTENT_CHANGED | FILE_ADDED | FILE_DELETED | MOVED | CONFIG_CHANGED | ROOTS_CHANGED | INDEXING_STATE
- `file`: VirtualFile (when applicable)
- `reason`: FLUID | TYPOSCRIPT | SITE_SETTINGS | ROOTS | OTHER
- `editorContext?`: active editor doc (for unsaved outline rebuild only)

The queue hands batched events to an **Updater** service that computes **ImpactedSets** and runs sub‑tasks in this order (read actions only):
1) **Rebuild IncludeIndex entries** for changed Fluid files.  
2) **Rebuild Contexts/RootPathSets** if any config changed.  
3) **Rebuild ImplementationCatalog entries** affected by path changes.  
4) **Re‑resolve EffectiveResolutionCache** for impacted `(Context, LogicalName, Kind)`.  
5) **Invalidate FlattenCache** for impacted entry points (see 5.3).  
6) **Notify UI** (toolwindows, gutters, decorators) to refresh on the EDT.

### 5.2 Impact analysis (files)

Given a set of changed *files*:

- If **Fluid file** (template/layout/partial):
  - Re‑parse and update `IncludeIndex[file]`. Compute **edge diff** = {removedEdges, addedEdges}.
  - Map the file to its **LogicalName** (infer from relative path within known root paths; else `null`).
  - If the file defines a **LogicalName** (i.e., it is an Implementation candidate):
    - For each **Context** where this file is within a `RootPathSet` of matching `Kind`, recompute `EffectiveResolutionCache(ctx, name, kind)`:
      - **On ADD**: if the new file becomes **higher priority** than the previous effective one → mark **override introduced**.
      - **On DELETE**: if it was the **effective** implementation → attempt **fallback** to next candidate; else no-op.
      - **On MOVE/RENAME**: treat as delete+add; if still within the same priority tier, the effective file may remain unchanged.
  - Compute **ImpactedLogicalNames**:
    - The file’s own logical name (if any).
    - Any logical names **referenced** by its callsites (from `IncludeIndex[file]`).

- If **TypoScript/site settings** file:
  - Re‑parse affected **Contexts** and produce an **old vs new RootPathSet diff**. For each `(ctx, kind, logicalName)` whose candidate list changed:
    - Rebuild `ImplementationCatalog`, update `EffectiveResolutionCache`, and record **ImpactedLogicalNames**.

### 5.3 Impact analysis (entry points / flatten)

For each **ImpactedLogicalName** and **Context**:
- From `ReverseIncludeIndex[name]`, gather **all callsites** and resolve to the **effective implementation** in that context.
- Walk **upward to entry points** using the reverse map (parents of parents…).  
- **Invalidate** `FlattenCache` for each impacted entry point. (Keep a `dependsOnFiles[]` list in each FlattenResult to allow additional invalidation by file.)

> Optimization: maintain a `LogicalName → EntryPointsByContext` memo to shortcut the upward traversal; update it incrementally using the edge diff.

---

## 6) Context & path set recomputation

Implement a `ContextManager` that can rebuild **just the contexts** affected by config changes.

- When a **TypoScript** or **site settings YAML** changes:
  1) Parse only the changed file and its include/import graph (if applicable).
  2) Reconstruct `RootPathSet` for the impacted ContextIds.
  3) Emit a **ContextChanged** event with prior & next RootPathSets.
  4) The Updater uses the diff to recompute only those `(Context, Kind, LogicalName)` whose **candidate lists** changed.

- Handle **priority semantics** precisely (reverse‑order search; first hit wins).

---

## 7) Unsaved edits: live outline without touching indexes

- For the **active editor** Fluid file, on `DocumentListener.change`:
  - Use `PsiDocumentManager.commitDocument(doc)` then parse **PSI only** (no FileBasedIndex).
  - Recompute a **temporary flattened tree** and heading outline **starting at this file** (or its entry point if determinable), with **source maps** pointing into the unsaved PSI.
  - UI marks these results as *“Unsaved (preview)”* and replaces them after file save triggers the normal pipeline.

This ensures instant feedback while keeping indexing stable.

---

## 8) UI behaviour on live updates

- **Parents & Overrides panel**
  - Auto‑refresh rows that involve impacted files/contexts.
  - Show badges for state transitions:
    - **Override introduced** (new higher‑priority implementation now active).
    - **Override removed** (fallback to lower tier).
    - **Unresolved** (no implementation found in any path).
  - Provide “Show changes” mini‑diff (old → new implementation path).

- **Heading Outline**
  - Soft‑refresh when only unrelated nodes changed; hard‑refresh when any included file or resolution changed.
  - Preserve expansion/cursor when possible.

- **Editor gutters/decorators**
  - `<f:render>` and `<f:layout>` gutters re‑resolve **on the fly** when the resolution cache updates.

- **Status & commands**
  - Status chip: “Graph updated: 0.23s ago” (auto‑throttled).  
  - Command: **Rebuild All** (full rescan) for safety; show progress.

---

## 9) Failure modes & recovery

- **During Dumb Mode**: Defer heavy recomputation; show a banner “Indexing… results may be stale.” Auto‑refresh after smart mode resumes.
- **Parse errors**: Mark file as **degraded**; carry forward previous edges if available; annotate UI with a warning.
- **Cycles** in include graph: Detect and short‑circuit with a diagnostic node in the flattened tree.
- **Vendor directory changes**: Be robust to massive event bursts (composer updates). Coalesce events; recompute only once per affected context set.

---

## 10) Performance & threading

- All scanners run in **ReadAction**; UI updates on EDT.  
- Use **NonBlockingReadAction** for flatten/audit with `expireWith(project)` and `finishOnUiThread`.  
- Use a **MergingUpdateQueue** (single queue per project) to debounce to ~250ms idle time.  
- Caches keyed by **VirtualFile URL + modification stamp** (and ContextId for resolution).  
- Track **FlattenResult.dependsOnFiles[]** to invalidate quickly after targeted changes.  
- Unit test large change bursts (1000+ file changes) to keep UI responsive.

---

## 11) Edge case semantics (adds/deletes)

### 11.1 Add a new partial in a higher‑priority root path
- It becomes the **effective** implementation for its logical name in that context.
- Updater marks **override introduced**. All entry points including that logical name become **impacted** and get re‑flattened.
- The old (lower‑tier) file remains in the ImplementationCatalog for “Show all candidates”.

### 11.2 Delete the currently effective partial
- Updater removes it from ImplementationCatalog and **re‑resolves** to the next candidate.
- If found → **override removed (fallback)**; if none → **unresolved**, annotate all callsites and outline with placeholders.
- Parents panel lists all direct parents with red markers and quick‑fix links.

### 11.3 Move/rename a partial within the same root path
- If its **logical name** (derived from relative path) changes, treat as delete+add:
  - Old name: fallback or unresolved.
  - New name: may activate as an override depending on priority.
- If only directory component changes but logical name mapping rules keep it the same, recompute yet likely no functional change.

### 11.4 Delete or add a layout/template
- Similar to partials but applied to the `layout`/`template` root lists and their referencing edges.

---

## 12) Minimal pseudo‑code

```kotlin
class GraphUpdater(project: Project) {
  private val queue = MergingUpdateQueue("FluidGraph", 250, true, null, project)
  
  fun onEvents(events: List<ChangeEvent>) = queue.queue(Update.create("batch") { process(events) })

  private fun process(events: List<ChangeEvent>) = ReadAction.run<RuntimeException> {
    val impact = Impact()
    for (e in events) when (e.kind) {
      FILE_ADDED, CONTENT_CHANGED, MOVED -> handleFluidOrConfigChange(e, impact)
      FILE_DELETED -> handleDeletion(e, impact)
      CONFIG_CHANGED, ROOTS_CHANGED -> handleConfigChange(e, impact)
      INDEXING_STATE -> handleIndexingState(e)
    }
    recompute(impact)
    notifyUi(impact)
  }

  private fun recompute(impact: Impact) {
    refreshIncludeIndex(impact.changedFluidFiles)
    rebuildContextsIfNeeded(impact.changedContexts)
    updateImplementationCatalog(impact.affectedNamesByContext)
    reResolveEffective(impact.affectedNamesByContext)
    invalidateFlatten(impact.impactedEntryPoints)
  }
}
```

---

## 13) Testing matrix (adds/deletes/live)

1. **Override introduction**: Add `EXT:site/…/Partials/Nav/Breadcrumb.html` (higher tier) → check that effective impl switches from vendor to site; outline and parents refresh; badge shown.
2. **Fallback**: Delete the above file → effective impl falls back to vendor; outline refresh; “override removed” badge.
3. **Unresolved path**: Delete vendor default with no fallback → callsites show unresolved, outline has placeholders.
4. **Rename logical name**: Move `Nav/Breadcrumb.html` to `Navigation/Breadcrumb.html` → old references unresolved; new references resolved if callsites update; parents panel shows diffs.
5. **TypoScript change**: Swap order of `partialRootPaths` → verify re‑resolution flips active implementation accordingly.
6. **Burst change**: Simulate 100 files changed under `vendor/` → single coalesced refresh; UI remains responsive.
7. **Unsaved edits**: Type `<h3>` into a partial and see live outline adjust in the active editor before save.
8. **Dumb Mode**: Trigger indexing; ensure heavy recomputes pause and auto‑resume.

---

## 14) Developer ergonomics & toggles

- **Settings:**
  - Enable/disable **Live Updates**
  - Debounce window (100–1000ms)
  - Include vendor directory in watch (on by default)
- **Commands:**
  - Rebuild All
  - Show Impacted Entry Points for <current file>
  - Toggle “Unsaved Preview”
- **Diagnostics:**
  - Show cache sizes, last update time, and number of impacted entry points in the Status panel.

---

## 15) Deliverables checklist for this feature

- [ ] BulkFileListener wired; filters for Fluid/TypoScript/YAML.
- [ ] DocumentListener + unsaved PSI outline for active editor.
- [ ] ContextManager with incremental rebuild from config diffs.
- [ ] ImplementationCatalog with reverse‑order search semantics.
- [ ] EffectiveResolutionCache recomputation and events.
- [ ] FlattenCache with `dependsOnFiles[]` and invalidations.
- [ ] Parents & Overrides toolwindow auto‑refresh + badges.
- [ ] Heading Outline auto‑refresh; state preservation.
- [ ] Dumb Mode guardrails and smart‑mode auto‑refresh.
- [ ] Unit tests covering §13 matrix.

---

### Notes for the implementer (GPT‑5 High)

- Be strict about **reverse‑order search** and **first hit wins** for path arrays.
- Prefer **PSI parsing** for instant editor feedback; prefer **indexes** for cross‑project queries.
- Always compute on read actions; keep UI updates on EDT; throttle via MergingUpdateQueue.
- Build clear **diff objects** (old→new resolution) to drive accurate UI changes without full redraws.
