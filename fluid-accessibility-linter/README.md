# Fluid Accessibility Linter for PhpStorm

A PhpStorm plugin that provides real-time accessibility linting for TYPO3 Fluid template files.

## Features

### Current Implementation

- **Missing Alt Text Detection**: Identifies `<img>` tags and `f:image` ViewHelpers without alt attributes
- **Invalid List Structure**: Detects when `<ul>` or `<ol>` elements contain invalid direct children (only `<li>` allowed)
- **Quick Fixes**: Automatic fixes to add missing attributes or wrap content in proper elements
- **Real-time Feedback**: Instant highlighting of accessibility issues while coding

## Detected Issues

1. **Images without alt text**
   - HTML: `<img src="...">` → Warning: Missing alt attribute
   - Fluid: `<f:image src="..." />` → Warning: Missing alt attribute

2. **Invalid list structure**
   - Direct text in lists: `<ul>Text here</ul>` → Error
   - Non-li elements: `<ul><div>...</div></ul>` → Error

## Installation

### Building from Source

1. Clone the repository
2. Open terminal in project directory
3. Run: `./gradlew buildPlugin`
4. The plugin ZIP will be in `build/distributions/`

### Installing in PhpStorm

1. Open PhpStorm Preferences
2. Go to Plugins → ⚙️ → Install Plugin from Disk
3. Select the built ZIP file
4. Restart PhpStorm

## Usage

The plugin automatically activates for all `.html` files in your project. Accessibility issues will be highlighted with:
- **Yellow underline**: Warnings (e.g., missing alt text)
- **Red underline**: Errors (e.g., invalid HTML structure)

Click on the highlighted issue and press `Alt+Enter` to see available quick fixes.

## Rule Profiles (Presets) and Settings

- Open `Settings | Tools | Fluid Accessibility`.
- Toggle rules on/off and adjust severity per rule.
- Apply a preset profile via the Preset selector:
  - `WCAG 2.1 AA`: Balanced defaults with critical issues as errors.
  - `WCAG 2.1 AAA`: Stricter than AA; more warnings elevated.
  - `Strict QA`: Treat most rules as errors for CI/QA.
  - `Relaxed Dev`: Lower severities to reduce noise during development.
  - `Fluid-Heavy`: Emphasizes Fluid ViewHelper checks.
- Import/Export: Use the buttons to import/export profiles as JSON.
- Project file: If `a11y-profile.json` exists in the project root, it is auto‑loaded when opening settings.
- Sample profile: See `fluid-accessibility-linter/a11y-profile.json` for a ready-made starting point.

Example profile JSON shape:
```
{
  "enabled": { "img-alt-text": true, "list-structure": true },
  "severity": { "img-alt-text": "ERROR", "link-text": "WARNING" },
  "config": { "link-text": { "maxLength": "100" } }
}
```

## Architecture

- Universal inspection: The plugin now registers a single rule‑engine based inspection (`Universal Fluid Accessibility Check`) in `plugin.xml`, which aggregates all checks. Legacy inspections remain available during the migration period.
- No custom file type in release: The main `plugin.xml` targets `language="HTML"` only. The alternative `plugin-universal.xml` (with custom Fluid language/file type) is reserved for internal experiments and must not be used for release builds.
- Performance: Results are cached per file and invalidated on edits, rule changes, or settings updates to keep inspections fast while staying accurate.

## Roadmap for Future Versions

### v1.1.0 - Image Checking
- Decorative image detection (alt="" validation)
- SVG accessibility attributes
- Figure/figcaption validation

Note: As of v1.4.0 these capabilities are merged into the base image inspection (no separate "Enhanced" variant).

### v1.2.0 - Form Accessibility
- Label association for form inputs
- Fieldset/legend for radio groups
- Required field indicators
- Error message associations

### v1.3.0 - Navigation & Structure
- Heading hierarchy validation (h1→h2→h3)
- Landmark regions (main, nav, aside)
- Skip links detection
- Link text quality ("click here" warnings)

### v1.4.0 - ARIA Support
- ARIA attribute validation
- Role attribute checking
- ARIA-label vs visible text
- Interactive element requirements

### v2.0.0 - Advanced Features
- WCAG 2.1 AA/AAA level configuration
- Custom rule sets per project
- Accessibility report generation
- Integration with TYPO3 backend checks

## Development

### Project Structure
```
src/main/java/com/typo3/fluid/linter/
├── FluidLanguage.java              # Language definition
├── FluidFileType.java              # File type registration
├── FluidParser.java                # Basic parser
├── inspections/
│   ├── MissingAltTextInspection.java
│   └── InvalidListStructureInspection.java
└── quickfixes/
    └── (Quick fix implementations)
```

### Adding New Rules

1. Create new class extending `LocalInspectionTool`
2. Implement pattern matching for Fluid syntax
3. Register in `plugin.xml`
4. Add quick fixes if applicable

### Testing

Run test templates through the plugin:
```bash
./gradlew runIde
```

Then open `test-templates/sample-with-issues.html` to see the linting in action.

### Regex Guidelines

When matching HTML/Fluid with regex in inspections, follow these conventions:

- Attribute boundaries: use `\b` before names (e.g., `\brole\s*=`, `\baria-label\s*=`) to avoid partial matches.
- Attribute clusters: match "any attrs" with `[^>]*` instead of quote-heavy negations.
- Values: match quotes portably with `[\"']` and contents with `[^\"']*`.
- Roles/ARIA: prefer patterns like `\brole\s*=\s*[\"']([^\"']+)[\"']` and `\baria-label(?:ledby)?\s*=\s*[\"']([^\"']+)[\"']`.
- IDs/classes: add word boundaries (`\bid\b`, `\bclass\b`) when filtering on specific attributes.
- Content spans: avoid giant "start-tag…end-tag" regex. Instead, find the opening tag and compute the end with parsing helpers (see `findElementEnd(...)`).
- Escaping: do not over-escape quotes. Only escape where Java strings require (`"` becomes `\"`).
- Flags: add `Pattern.CASE_INSENSITIVE` (and `DOTALL` only when the pattern truly spans newlines).
- Helpers: favor `getAttributeValue(tag, name)` and `hasAttribute(tag, name)` from `FluidAccessibilityInspection` over ad‑hoc regex.
- Performance: compile patterns as `private static final Pattern` and keep them specific to reduce backtracking.

Example snippets:

- Any `<ul|ol>` with menu intent: `"<(?:ul|ol)[^>]*\b(class|role)\b[^>]*>"`
- Redundant list role on `<ul>`: `"<ul[^>]*" + ROLE_LIST_PATTERN.pattern() + "[^>]*>"`

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Add tests for new inspections
4. Submit a pull request

## License

MIT License - See LICENSE file for details

## Support

For issues or feature requests, please use the GitHub issue tracker.
