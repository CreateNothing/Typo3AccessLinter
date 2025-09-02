package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class ListSemanticInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenListHasNoLi() {
        String html = "<ul><div>Item</div></ul>";
        var highlights = highlight(html, new ListSemanticInspection());
        assertHighlightsContain(highlights, "List <ul> has no <li> elements");
    }

    // Note: some internal validations may consolidate errors; rely on general no-<li> check.

    @Test
    public void testShouldWarn_whenDlMissingDd() {
        String html = "<dl><dt>Term</dt></dl>";
        var highlights = highlight(html, new ListSemanticInspection());
        assertHighlightsContain(highlights, "Description list <dl> must contain both <dt> and <dd> elements");
    }

    @Test
    public void testShouldWarn_whenEmptyListItem() {
        String html = "<ul><li></li></ul>";
        var highlights = highlight(html, new ListSemanticInspection());
        assertHighlightsContain(highlights, "Remove empty list items; each <li> should have content");
    }

    @Test
    public void testShouldWarn_whenUlHasRedundantRoleList() {
        String html = "<ul role=\"list\"><li>A</li></ul>";
        var highlights = highlight(html, new ListSemanticInspection());
        assertHighlightsContain(highlights, "Redundant role='list' on <ul> element");
    }

    @Test
    public void testShouldWarn_whenSingleItemList() {
        String html = "<ul><li>One</li></ul>";
        var highlights = highlight(html, new ListSemanticInspection());
        assertHighlightsContain(highlights, "List with only one item. Consider if a list is appropriate here");
    }
}
