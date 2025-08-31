package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class TabPanelInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenTablistMissingLabelAndTabs() {
        String html = "<div role=\"tablist\"></div>";
        var highlights = highlight(html, new TabPanelInspection());
        assertHighlightsContain(highlights,
                "Give the tablist a short label",
                "Ensure the tablist contains at least one element with role='tab'");
    }

    @Test
    public void testShouldWarn_whenTabMissingSelectedAndControls() {
        String html = "<div role=\"tab\">Tab</div>";
        var highlights = highlight(html, new TabPanelInspection());
        assertHighlightsContain(highlights,
                "Add aria-selected to the tab",
                "Add aria-controls on the tab pointing to its tabpanel");
    }

    @Test
    public void testShouldWarn_whenPanelMissingIdAndLabelledByAndTabindex() {
        String html = "<div role=\"tabpanel\"></div>";
        var highlights = highlight(html, new TabPanelInspection());
        assertHighlightsContain(highlights,
                "Add an id to the tabpanel",
                "Add aria-labelledby on the tabpanel pointing to its tab",
                "Make the tab panel focusable (tabindex='0')");
    }

    @Test
    public void testShouldWarn_whenMultipleTabsSelected() {
        String html = "<div role=\"tab\" aria-selected=\"true\"></div><div role=\"tab\" aria-selected=\"true\"></div>";
        var highlights = highlight(html, new TabPanelInspection());
        assertHighlightsContain(highlights, "Have only one tab with aria-selected='true'");
    }
}
