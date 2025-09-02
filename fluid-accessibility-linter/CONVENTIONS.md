# Messaging and Quick-Fix Conventions

This document standardizes user‑facing phrasing and quick‑fix naming across the plugin.

## Phrasing vs. Severity
- ERROR: start with “Must …” or “Must not …”.
- WARNING: start with “Should …” or “Should not …”.
- WEAK_WARNING: prefer “Consider …”.
- INFORMATION: neutral/descriptive tone (e.g., “Language change detected …”).

Notes:
- Keep existing substrings relied upon by tests (e.g., “missing alt attribute”, “Heading level jumps”).
- When a rule’s severity is WARNING but the issue feels critical, prefer “Should …” to avoid mismatch (RuleEngine assigns severity per rule id).

## Quick-Fix Naming
- Action titles: imperative verb + object (e.g., “Add …”, “Remove …”, “Change …”, “Generate …”, “Wrap …”, “Mark …”).
- Family name: short, stable group label. Prefer domain categories used today:
  - “ARIA”, “Navigation”, “Forms”, “Images”, “Tables”, “Headings”, “Structure”, or a generic “Accessibility”.
- Preserve existing quick-fix texts used in tests (e.g., “Add alt attribute”, “Generate <thead> with <th> headers”).

## Examples
- ERROR: “Images must include an alt attribute”
- WARNING: “Navigation lists should be marked up with <ul> or <ol>”
- WEAK_WARNING: “Consider shortening link text (over 100 characters)”

## Application Guidance
- When updating messages, align the modal (“must/should/consider”) with the configured rule severity.
- If tests assert specific substrings, keep those tokens intact and adjust surrounding wording only when safe.
