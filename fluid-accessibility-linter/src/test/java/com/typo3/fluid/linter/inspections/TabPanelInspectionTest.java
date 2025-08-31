package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class TabPanelInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenTablistMissingLabelAndTabs() {
        String html = "<div role=\"tablist\"></div>";
        var highlights = highlight(html, new TabPanelInspection());
        assertHighlightsContain(highlights,
                "Tablist should have aria-label or aria-labelledby for context",
                "Tablist must contain at least one element with role='tab'");
    }

    @Test
    public void testShouldWarn_whenTabMissingSelectedAndControls() {
        String html = "<div role=\"tab\">Tab</div>";
        var highlights = highlight(html, new TabPanelInspection());
        assertHighlightsContain(highlights,
                "Tab must have aria-selected attribute",
                "Tab should have aria-controls pointing to its tabpanel");
    }

    @Test
    public void testShouldWarn_whenPanelMissingIdAndLabelledByAndTabindex() {
        String html = "<div role=\"tabpanel\"></div>";
        var highlights = highlight(html, new TabPanelInspection());
        assertHighlightsContain(highlights,
                "Tabpanel must have an id for aria-controls reference",
                "Tabpanel should have aria-labelledby pointing to its tab",
                "Tabpanel should have tabindex='0' for keyboard accessibility");
    }

    @Test
    public void testShouldWarn_whenMultipleTabsSelected() {
        String html = "<div role=\"tab\" aria-selected=\"true\"></div><div role=\"tab\" aria-selected=\"true\"></div>";
        var highlights = highlight(html, new TabPanelInspection());
        assertHighlightsContain(highlights, "Only one tab should have aria-selected='true'");
    }
}
