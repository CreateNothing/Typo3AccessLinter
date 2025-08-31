package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class InvalidListStructureInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenNonLiDirectChild() {
        String html = "<ul><div>Item</div></ul>";
        var highlights = highlight(html, new InvalidListStructureInspection());
        assertHighlightsContain(highlights, "should only contain <li> elements or control flow ViewHelpers as direct children");
    }

    @Test
    public void testShouldWarn_whenDirectTextInList() {
        String html = "<ol>Text<li>Item</li></ol>";
        var highlights = highlight(html, new InvalidListStructureInspection());
        assertHighlightsContain(highlights, "<ol> contains direct text content. Text must be wrapped in <li> elements");
    }

    @Test
    public void testShouldAllow_ControlFlowViewHelpersAsDirectChildren() {
        String html = "<ul><f:if condition=\"1\"><li>A</li></f:if></ul>";
        var highlights = highlight(html, new InvalidListStructureInspection());
        assertNoHighlightsContaining(highlights, "should only contain <li>");
    }
}
