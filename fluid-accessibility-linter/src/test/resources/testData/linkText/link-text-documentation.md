# WCAG Link Text Inspection - Documentation

## Overview
This document explains when link text accessibility issues apply according to WCAG 2.1 Success Criterion 2.4.4 (Link Purpose in Context).

## When Link Text Issues Apply

### 1. **Always Flag as Issues**
These patterns should ALWAYS be flagged as accessibility violations:

#### Generic/Vague Link Text (Standalone)
- "click here"
- "here"  
- "read more"
- "learn more"
- "more"
- "click"
- "download"
- "link"
- "this link"
- "this page"

**Why:** Screen reader users often navigate by tabbing through links or requesting a list of all links on a page. These generic phrases provide no context about the link destination when heard in isolation.

#### Empty or Meaningless Links
- Empty link text (`<a href="/page"></a>`)
- Only whitespace (`<a href="/page">   </a>`)
- Single punctuation (`<a href="/next">></a>`)
- Single characters without clear meaning (`<a href="/info">i</a>`)

**Why:** Provides no information to any user about the link's purpose.

#### Raw URLs (especially complex ones)
- `https://example.com/page?id=123&ref=abc&utm_source=email`
- Long, parameter-heavy URLs
- URLs with encoded characters

**Why:** URLs are often not human-readable and screen readers may read every character, making them incomprehensible.

### 2. **Context-Dependent Issues**

These phrases MAY be acceptable if sufficient programmatic context is provided:

#### Contextual Phrases
- "read more"
- "learn more"
- "continue reading"
- "view more"
- "details"
- "more info"

**When Acceptable:**
- When used with `aria-label` providing full context
- When used with `aria-labelledby` referencing a heading
- When used with `aria-describedby` for additional context
- When the link is in a table cell with descriptive headers
- When immediately preceded by descriptive text in the same sentence/paragraph

**Example of Acceptable Use:**
```html
<h3 id="article-title">New Accessibility Features</h3>
<p>We've released important updates...</p>
<a href="/article" aria-labelledby="article-title">Read more</a>
```

### 3. **Special Considerations**

#### File Downloads
**Must Include:**
- File type (PDF, DOC, etc.)
- File size when significant
- Clear description of content

**Good:** "Download Annual Report 2024 (PDF, 2.3 MB)"
**Bad:** "Download"

#### Image Links
- Alt text serves as link text
- Must describe the link destination, not the image
- Empty alt text on linked images is a violation

#### Repeated Links in Lists/Tables
- Each link should be unique or have unique programmatic context
- Multiple "Edit" or "Delete" links need additional context

#### Form Actions
- Should clearly state the action outcome
- "Submit" alone may be insufficient - prefer "Submit application"

## When Link Text Issues DON'T Apply

### 1. **Navigation and Menu Items**
Simple, standard navigation terms are generally acceptable:
- "Home"
- "About"
- "Contact"
- "Products"
- "Services"

**Why:** These are understood conventions and are typically grouped in navigation regions.

### 2. **Skip Links**
Utility navigation links are acceptable with standard phrasing:
- "Skip to main content"
- "Skip to navigation"

### 3. **When Visual Context is Programmatically Associated**
Links are acceptable when:
- Properly implemented `aria-label` provides full context
- `aria-labelledby` references descriptive elements
- Link is in a labeled region or landmark

### 4. **Single-Page Applications with Clear Context**
In some SPAs where the link context is extremely clear from the immediate UI state, shorter link text may be acceptable if:
- The page has a single, clear purpose
- The link's destination is obvious from the page context
- Additional ARIA attributes provide context

### 5. **Breadcrumb Navigation**
Short link text in breadcrumbs is acceptable as the hierarchical structure provides context.

## Implementation Notes for LinkTextInspection.java

### Severity Levels
1. **ERROR** - Empty links, single character links
2. **WARNING** - Generic phrases like "click here", "read more"
3. **INFO** - Could benefit from more context but has some meaning

### Detection Strategy
1. Check for exact matches to NON_DESCRIPTIVE_PHRASES (case-insensitive)
2. Check for CONTEXTUAL_PHRASES and verify if proper ARIA attributes exist
3. Flag raw URLs, especially with parameters
4. Detect missing file type information for common file extensions
5. Check image links for empty or generic alt text

### Quick Fix Suggestions
1. For "click here" - suggest using the surrounding context
2. For file downloads - add file type and size
3. For repeated links - suggest adding aria-label with unique context
4. For image links - suggest descriptive alt text

## Testing Approach

### Test Files Created
1. **link-text-issues.html** - Contains all violation patterns
2. **link-text-valid.html** - Contains best practice examples

### Test Coverage Should Include
- All items from NON_DESCRIPTIVE_PHRASES
- All items from CONTEXTUAL_PHRASES (with and without ARIA)
- Edge cases (empty, whitespace, punctuation)
- Fluid ViewHelper variations
- Different language contexts
- Various HTML contexts (tables, lists, forms)