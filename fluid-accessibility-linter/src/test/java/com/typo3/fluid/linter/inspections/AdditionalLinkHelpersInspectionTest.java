package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class AdditionalLinkHelpersInspectionTest extends BaseInspectionTest {

    // f:link.email
    @Test
    public void testShouldWarn_whenLinkEmailHasGenericText() {
        String html = "<f:link.email email=\"info@example.com\">Click here</f:link.email>";
        var highlights = highlight(html, new LinkTextInspection());
        assertHighlightsContain(highlights, "not descriptive");
    }

    @Test
    public void testShouldNotWarn_whenLinkEmailHasDescriptiveText() {
        String html = "<f:link.email email=\"info@example.com\">Email customer support</f:link.email>";
        var highlights = highlight(html, new LinkTextInspection());
        assertNoHighlightsContaining(highlights, "not descriptive");
    }

    // f:link.file
    @Test
    public void testShouldWarn_whenLinkFileHasGenericText() {
        String html = "<f:link.file file=\"fileadmin/doc.pdf\">here</f:link.file>";
        var highlights = highlight(html, new LinkTextInspection());
        assertHighlightsContain(highlights, "not descriptive");
    }

    @Test
    public void testShouldNotWarn_whenLinkFileIsDescriptive() {
        String html = "<f:link.file file=\"fileadmin/pricing.pdf\">Download pricing PDF (120 kB)</f:link.file>";
        var highlights = highlight(html, new LinkTextInspection());
        assertNoHighlightsContaining(highlights, "not descriptive");
    }

    // f:link.typolink
    @Test
    public void testShouldWarn_whenTypolinkHasGenericText() {
        String html = "<f:link.typolink parameter=\"123\">Read more</f:link.typolink>";
        var highlights = highlight(html, new LinkTextInspection());
        assertHighlightsContain(highlights, "needs context");
    }

    @Test
    public void testShouldNotWarn_whenIconOnlyTypolinkHasAriaLabel() {
        String html = """
            <f:link.typolink parameter=\"123\" aria-label=\"Open contact page\">
                <svg class=\"icon\" width=\"16\" height=\"16\"><path d=\"M0 0h16v16H0z\"/></svg>
            </f:link.typolink>
            """;
        var highlights = highlight(html, new LinkTextInspection());
        assertNoHighlightsContaining(highlights, "Icon-only link must have aria-label");
    }
}

