package com.typo3.fluid.linter.inspections;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Placeholder tests for core:icon in frontend contexts.
 * TODO: Implement inspection support to treat <core:icon> as an icon descendant
 *       for links/buttons without visible text, requiring aria-label.
 */
public class CoreIconViewHelperAccessibilityTest extends BaseInspectionTest {

    @Ignore("Pending: implement core:icon detection in LinkTextInspection/AriaLabelValidationInspection")
    @Test
    public void testIconOnlyLinkWithCoreIconNeedsAriaLabel() {
        String html = "<a href=\"/#\"><core:icon identifier=\"actions-close\" /></a>";
        var highlights = highlight(html, new LinkTextInspection());
        assertHighlightsContain(highlights, "Icon-only link must have aria-label");
    }

    @Ignore("Pending: implement core:icon detection in AriaLabelValidationInspection")
    @Test
    public void testIconOnlyButtonWithCoreIconNeedsAriaLabel() {
        String html = "<button><core:icon identifier=\"actions-settings\" /></button>";
        var highlights = highlight(html, new AriaLabelValidationInspection());
        assertHighlightsContain(highlights, "Icon-only button needs accessible text");
    }
}

