# Fluid A11y Test Fixtures

Synthetic TYPO3 Fluid project used by automated tests to exercise:
- Include parsing (partials, sections, layouts)
- Root path resolution & overrides (vendor vs site_a)
- Graph building & reverse include index
- Flattening & heading extraction
- Live adds/deletes/renames & config changes
- Edge cases: dynamic includes, unresolved names, recursion

Notes:
- Paths are shaped like real TYPO3 projects but kept minimal.
- TypoScript shows FLUIDTEMPLATE usage; site settings provide a simple override.
