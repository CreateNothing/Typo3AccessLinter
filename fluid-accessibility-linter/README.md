# Fluid Accessibility Linter for PhpStorm

A PhpStorm plugin that provides real-time accessibility linting for TYPO3 Fluid template files.

## Features

### Current Implementation (v1.0.0)

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

## Roadmap for Future Versions

### v1.1.0 - Enhanced Image Checking
- Decorative image detection (alt="" validation)
- SVG accessibility attributes
- Figure/figcaption validation

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