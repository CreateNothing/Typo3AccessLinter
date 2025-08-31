package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class UniversalAccessibilityInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldHighlightMissingAlt_viaRuleEngine() {
        String html = "<img src=\"x.jpg\">";
        var highlights = highlight(html, new UniversalAccessibilityInspection());
        assertHighlightsContain(highlights, "Image missing alt attribute");
    }

    @Test
    public void testShouldHighlightHeadingJump_viaRuleEngine() {
        String html = "<h1>T</h1><h3>Skip</h3>";
        var highlights = highlight(html, new UniversalAccessibilityInspection());
        assertHighlightsContain(highlights, "Heading level jumps from h1 to h3");
    }
}
