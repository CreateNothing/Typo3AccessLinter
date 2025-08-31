package com.typo3.fluid.linter.inspections;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Placeholder tests for content-transforming helpers that may affect semantics.
 * Covered helpers: f:sanitize.html, f:transform.html, f:format.stripTags, f:format.nl2br,
 * f:format.crop, f:format.html.
 *
 * These tests are marked @Ignore until specific inspections are implemented
 * to catch semantic regressions (e.g., stripping headings or lists, replacing
 * paragraphs with <br>, etc.).
 */
public class ContentSanitizationHelpersTest extends BaseInspectionTest {

    @Ignore("Pending: implement semantic-safety checks for content filters")
    @Test
    public void testNl2brShouldNotReplaceStructuredParagraphs() {
        String html = "<f:format.nl2br>Line 1\n\nLine 2</f:format.nl2br>";
        var highlights = highlight(html, new HeadingHierarchyInspection(), new ListSemanticInspection());
        // Expect informational advice once implemented
        assertNoHighlightsContaining(highlights, "replaced structured content");
    }

    @Ignore("Pending: implement semantic-safety checks for content filters")
    @Test
    public void testStripTagsShouldWarnIfRemovingHeadings() {
        String html = "<f:format.stripTags><h2>Title</h2>Body</f:format.stripTags>";
        var highlights = highlight(html, new HeadingHierarchyInspection());
        // Expect a warning once implemented
        assertHighlightsContain(highlights, "Removing headings may harm accessibility");
    }

    @Ignore("Pending: implement semantic-safety checks for content filters")
    @Test
    public void testSanitizeHtmlAllowsEssentialAttributes() {
        String html = "<f:sanitize.html><a href=\"/#\" aria-label=\"Home\">Home</a></f:sanitize.html>";
        var highlights = highlight(html, new LinkTextInspection(), new AriaLabelValidationInspection());
        // Expect no false positives once implemented
        assertNoHighlightsContaining(highlights, "stripped required attribute");
    }
}

