package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class TableAccessibilityInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenLargeDataTableHasNoHeaders() {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < 6; i++) rows.append("<tr><td>A</td><td>B</td></tr>");
        String html = "<table>" + rows + "</table>";
        var highlights = highlight(html, new TableAccessibilityInspection());
        assertHighlightsContain(highlights, "Data table should have header cells (<th>) to describe the data");
    }

    @Test
    public void testShouldNotWarnHeaders_whenLayoutTablePresentationRole() {
        String html = "<table role=\"presentation\"><tr><td>A</td><td>B</td></tr></table>";
        var highlights = highlight(html, new TableAccessibilityInspection());
        assertNoHighlightsContaining(highlights, "header cells", "<th>");
    }

    @Test
    public void testShouldWarn_whenTablePurposeUnclear() {
        String html = "<table><tr><td>A</td><td>B</td></tr></table>";
        var highlights = highlight(html, new TableAccessibilityInspection());
        assertHighlightsContain(highlights, "Table purpose unclear. Add role='presentation' for layout or proper headers for data tables");
    }
}
