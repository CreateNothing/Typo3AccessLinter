package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class FluidLinkViewHelperVariantsTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenFluidLinkPageHasGenericText() {
        String html = "<f:link.page pageUid=\"1\">Click here</f:link.page>";
        var highlights = highlight(html, new LinkTextInspection());
        assertHighlightsContain(highlights, "not descriptive");
    }

    @Test
    public void testShouldWarn_whenFluidLinkExternalHasHere() {
        String html = "<f:link.external uri=\"https://example.com\">here</f:link.external>";
        var highlights = highlight(html, new LinkTextInspection());
        assertHighlightsContain(highlights, "not descriptive");
    }

    @Test
    public void testShouldNotWarn_whenIconOnlyFluidLinkHasAriaLabel() {
        String html = """
            <f:link.action action=\"show\" aria-label=\"View details\"> 
                <svg class=\"icon\" width=\"16\" height=\"16\"><path d=\"M0 0h16v16H0z\"/></svg>
            </f:link.action>
            """;
        var highlights = highlight(html, new LinkTextInspection());
        assertNoHighlightsContaining(highlights, "Link has no text content");
    }

    @Test
    public void testShouldWarn_whenNamespacedFluidLinkHasGenericText() {
        // Custom namespace with :link.page should be treated like link ViewHelper
        String html = "<my:link.page pageUid=\"1\">Read more</my:link.page>";
        var highlights = highlight(html, new LinkTextInspection());
        assertHighlightsContain(highlights, "needs context");
    }
}
