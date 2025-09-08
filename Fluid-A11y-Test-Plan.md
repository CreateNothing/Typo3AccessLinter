
# Fluid A11y Plugin — **Test Strategy & Spec**

**Purpose:** Hand‑off test plan for *GPT‑5 High* to validate the PhpStorm plugin that reconstructs TYPO3 Fluid compositions and audits heading structure (H1–H6), including **overrides**, **many‑to‑many parents**, and **live updates** (adds, deletes, renames, edits, config changes).

---

## 1) Test objectives

1. **Correct parsing & indexing** of Fluid includes: `<f:render partial="…">`, `section="…"`, and `<f:layout name="…">`.
2. **Context & root‑path resolution** (TypoScript + site settings) with **reverse‑order search**: first hit wins.
3. **Graph building** from entry points; **parents** (who includes me) & **entry points** (who ultimately uses me).
4. **Flattening** of composed templates (layouts + sections + partials), including **source maps**.
5. **Heading audit** correctness: duplicates of H1, level skips, missing H1.
6. **Live updates**: additions, deletions, renames/moves, content edits (unsaved & saved), config changes; coalesced updates; dumb‑mode behavior.
7. **Edge cases**: dynamic partials, unresolved names, cycles, nested sections, multiple contexts.
8. **Performance**: bounded recomputation and queue coalescing under bursts.

---

## 2) Test fixture overview

A small, synthetic project used by tests. Download the ready‑made fixtures archive alongside this plan.

**Structure (selected paths):**
```
FluidA11yFixtures/
  ext_vendor/
    Configuration/TypoScript/setup.typoscript
    Resources/Private/Layouts/Main.html
    Resources/Private/Templates/Page/Default.html
    Resources/Private/Templates/Page/Alt.html
    Resources/Private/Templates/Feature/SectionConsumer.html
    Resources/Private/Partials/Navigation/Breadcrumb.html
    Resources/Private/Partials/Navigation/Subcrumb.html
    Resources/Private/Partials/Card.html
  site_a/
    Configuration/TypoScript/setup.typoscript
    Resources/Private/Partials/Navigation/Breadcrumb.html
  site_settings/
    site_a/settings.yaml
  dynamic/
    Resources/Private/Templates/Feature/Dynamic.html
  cycles/
    Resources/Private/Partials/Loop/SelfInclude.html
  unresolved/
    Resources/Private/Templates/Feature/MissingPartial.html
  README-fixtures.md
```

> The fixtures encode both **FLUIDTEMPLATE** and **PAGE** TypoScript variants and a simple **site settings** override to exercise priority/override logic.

---

## 3) Test categories & representative cases

### 3.1 Unit tests (pure services)

**A. Root‑path resolution**
- **Given** the TypoScript in `ext_vendor` and `site_a`,
- **Then** `partialRootPaths` for the context must be `[EXT:ext_vendor/…/Partials, EXT:site_a/…/Partials]` **searched in reverse order** (i.e., `site_a` first).  
- **Also** verify that changing the numeric keys (e.g., `0`, `10`, `20`) affects priority as expected.

**B. Effective implementation (`resolve(ctx, name, kind)`)**
- **Given** `Navigation/Breadcrumb` and Context=page/FLUIDTEMPLATE,  
- **Then** result is `EXT:site_a/…/Breadcrumb.html` (override active).  
- **When** site_a file is missing, **Then** fallback is vendor file.

**C. Include parsing**
- Extract IncludeEdges from each fixture file:
  - `Default.html` includes `partial="Navigation/Breadcrumb"` and `layout="Main"`.
  - `Breadcrumb.html` includes `Navigation/Subcrumb` (nested).  
- Assert correct `isDynamic` for `dynamic/Dynamic.html`.

**D. Cycle detection**
- Detect recursion in `cycles/…/SelfInclude.html` and report gracefully.

**E. TypoScript/site settings parsers**
- Parse `setup.typoscript` and `settings.yaml` to contexts and path sets.

### 3.2 PSI/Index integration tests

**F. PSI reference resolution (navigation)**
- Caret on `partial="Navigation/Breadcrumb"` resolves to **effective** implementation file in the active context.

**G. Reverse include index (“parents”)**
- From `site_a/.../Breadcrumb.html` list **all callsites** (Default.html etc.) with file+line.

**H. Context inference**
- Files under `ext_vendor` map to the default FLUIDTEMPLATE context; confirm with ContextManager.

### 3.3 Flatten & audit

**I. Flatten composition with layout+sections**
- Entry point: `Templates/Page/Default.html`.  
- Expect DOM order includes vendor layout sections and inlined partials, producing heading outline:
  - H1 Vendor Page
  - H2 SiteA Breadcrumb (override)
  - H2 Intro

**J. Heading rules**
- Duplicate H1 (if introduced) is flagged.
- Level skip (e.g., H2→H4) is flagged.
- Missing H1 is flagged.

### 3.4 Live updates

**K. Add high‑priority override**
- **When** `site_a/…/Breadcrumb.html` is created,  
- **Then** effective implementation switches from vendor to site_a, reverse index updates, and outline recomputes for impacted entry points. Badge “override introduced”.

**L. Delete active override**
- **When** the site_a Breadcrumb is deleted,  
- **Then** fallback to vendor; outline recomputed; badge “override removed”.

**M. Rename/move**
- **When** Breadcrumb is moved to `Navigation/BreadcrumbNew.html`,  
- **Then** old logical name unresolved; new logical name not yet referenced; parents panel shows diffs.

**N. TypoScript change (priority swap)**
- **When** keys in `partialRootPaths` are swapped so vendor has higher priority,  
- **Then** resolution flips; outline recomputed.

**O. Unsaved edits (editor)**
- **Given** `Breadcrumb.html` open, **When** a new `<h3>` is typed,  
- **Then** temporary (unsaved) outline updates; after save, indexed outline matches.

**P. Dumb mode**
- **Given** forced Dumb Mode, **Then** heavy recomputations idle; **When** smart mode resumes, refresh triggers exactly once (coalesced).

**Q. Burst changes**
- Simulate 50 file changes under `ext_vendor/Resources/Private/Partials`. Ensure a single coalesced recomputation and responsive UI model.

---

## 4) Test skeletons (Kotlin, IntelliJ Platform Test Framework)

> These are **blueprints**; replace service names to match your implementation. Prefer `BasePlatformTestCase` + `CodeInsightTestFixture`.

```kotlin
// Base test wiring
abstract class FluidPluginTestBase : BasePlatformTestCase() {
  override fun getTestDataPath(): String = System.getProperty("fluid.testDataPath") ?: "testData"
  override fun setUp() {
    super.setUp()
    myFixture.copyDirectoryToProject("FluidA11yFixtures", "")
  }
  protected inline fun <T> write(block: () -> T): T =
    WriteCommandAction.runWriteCommandAction(project, java.util.concurrent.Callable<T> { block() })
}
```

**A. Root‑path resolution**

```kotlin
class RootPathResolutionTest : FluidPluginTestBase() {

  @Test fun `reverse order search picks site_a first`() {
    val cm = project.service<ContextManager>()
    val ctx = cm.contextForFile(findVFile("ext_vendor/Resources/Private/Templates/Page/Default.html"))
    val paths = cm.paths(ctx)
    assertTrue(paths.partials.last().path.endsWith("site_a/Resources/Private/Partials"))
    val resolver = project.service<Resolver>()
    val impl = resolver.resolve(ctx, LogicalName("Navigation/Breadcrumb"), Kind.PARTIAL)
    assertTrue(impl!!.file.path.contains("site_a/Resources/Private/Partials"))
  }
}
```

**B. PSI reference resolution**

```kotlin
class PsiReferenceResolutionTest : FluidPluginTestBase() {

  @Test fun `partial attribute resolves to effective implementation`() {
    val file = myFixture.copyFileToProject("ext_vendor/Resources/Private/Templates/Page/Default.html", "Default.html")
    myFixture.openFileInEditor(file)
    val offset = myFixture.editor.document.text.indexOf("partial=\"Navigation/Breadcrumb\"") + "partial=\"".length
    myFixture.editor.caretModel.moveToOffset(offset)
    val ref = myFixture.getReferenceAtCaretPositionWithAssertion()
    val resolved = ref.resolve()!!
    assertTrue(resolved.containingFile.virtualFile.path.contains("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html"))
  }
}
```

**C. Flatten & outline**

```kotlin
class FlattenOutlineTest : FluidPluginTestBase() {

  @Test fun `default page produces expected heading outline`() {
    val flattener = project.service<Flattener>()
    val ctxMgr = project.service<ContextManager>()
    val vFile = findVFile("ext_vendor/Resources/Private/Templates/Page/Default.html")
    val ctx = ctxMgr.contextForFile(vFile)
    val result = flattener.flatten(EntryPoint(vFile, ctx))
    val headings = result.headings.map { it.text to it.level }
    assertEquals(listOf("Vendor Page" to 1, "SiteA Breadcrumb" to 3, "Intro" to 2), headings)
  }
}
```

**D. Live add/delete**

```kotlin
class LiveOverrideTest : FluidPluginTestBase() {
  @Test fun `adding site override switches resolution`() {
    val cm = project.service<ContextManager>()
    val resolver = project.service<Resolver>()
    val ctx = cm.contextForFile(findVFile("ext_vendor/Resources/Private/Templates/Page/Default.html"))

    // Ensure vendor is active (delete site file if present)
    write {
      findVFileOrNull("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html")?.delete(this)
    }
    var impl = resolver.resolve(ctx, LogicalName("Navigation/Breadcrumb"), Kind.PARTIAL)
    assertTrue(impl!!.file.path.contains("ext_vendor/Resources/Private/Partials"))

    // Add high-priority override
    val text = "<nav aria-label=\"breadcrumb\"><h3>SiteA Breadcrumb</h3></nav>"
    write {
      createFile("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html", text)
    }
    // Flush debounced queue if your updater exposes a test hook
    project.service<GraphUpdater>().flushForTests()

    impl = resolver.resolve(ctx, LogicalName("Navigation/Breadcrumb"), Kind.PARTIAL)
    assertTrue(impl!!.file.path.contains("site_a/Resources/Private/Partials"))
  }
}
```

**E. Dumb mode**

```kotlin
class DumbModeTest : FluidPluginTestBase() {
  @Test fun `recompute deferred in dumb mode`() {
    val updater = project.service<GraphUpdater>()
    (DumbService.getInstance(project) as DumbServiceImpl).setDumb(true)
    write { createFile("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html", "<nav><h3>SiteA Breadcrumb</h3></nav>") }
    updater.flushForTests()
    // Expect no recompute while dumb
    assertFalse(updater.lastRunHadChanges())
    (DumbService.getInstance(project) as DumbServiceImpl).setDumb(false)
    updater.flushForTests()
    assertTrue(updater.lastRunHadChanges())
  }
}
```

> Notes:
> - Provide `findVFile(...)`, `findVFileOrNull(...)`, and `createFile(...)` helpers in the base class.
> - Expose test‑only methods like `GraphUpdater.flushForTests()` using `@TestOnly` to drain the MergingUpdateQueue.

---

## 5) Acceptance checks per test

For each case (A–Q) above, define **Given/When/Then** steps with concrete checks:
- Existence/content of expected files.
- Expected **Implementation** path for a logical name.
- Expected **parents** callsites with file + line numbers.
- Expected **heading sequence** and rule violations.
- Expected **badges/state transitions** in model objects (not the UI widgets).

---

## 6) Performance sanity tests

- **Coalescing under burst**: generate 50 create/edit events; assert the updater ran **once** (use a counter incremented by the recompute method).
- **Flatten time bound**: flatten an entry point with ~10 nested includes; assert completes under N ms on CI; treat as soft assertion if CI variance is high.

---

## 7) How to run

- Place fixtures under `testData/FluidA11yFixtures` or set `-Dfluid.testDataPath=/absolute/path/to/fixtures`.
- Run with Gradle:
  ```bash
  ./gradlew test
  ```
- In IDE: use “Gradle Test” or JUnit run configurations.

---

## 8) Deliverables (tests)

- [ ] Test base + helpers.
- [ ] Unit tests A–E.
- [ ] PSI/index tests F–H.
- [ ] Integration/flatten/audit tests I–J.
- [ ] Live update tests K–Q with test hooks to flush queues.
- [ ] Performance tests.
- [ ] CI job (GitHub Actions) running `./gradlew test` on Linux + Windows.

---

## 9) Extension ideas

- Property‑based tests for resolver invariants.
- Mutation tests for parsers (ensure include detection is robust to whitespace/attribute order).
- Synthetic **very deep** include chains to validate cycle detection & stack safety.

---

**End of Spec**
