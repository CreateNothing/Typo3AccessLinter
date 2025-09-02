package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class TranslateAndAltIntegrationTest extends BaseInspectionTest {

    @Test
    public void testShouldNotWarn_whenFluidImageAltUsesTranslate() {
        String html = "<f:image src=\"photo.jpg\" alt=\"{f:translate(key: 'image.alt.photo')}\" />";
        var highlights = highlight(html, new MissingAltTextInspection());
        assertNoHighlightsContaining(highlights, "missing alt");
    }

    @Test
    public void testShouldNotWarn_whenLinkHasTranslatedAccessibleName() {
        String html = """
            <f:link.page pageUid=\"1\" aria-label=\"{f:translate(key:'nav.home')}\">
                <svg class=\"icon\"></svg>
            </f:link.page>
            """;
        var highlights = highlight(html, new LinkTextInspection());
        assertNoHighlightsContaining(highlights, "Icon-only link must have aria-label");
    }
}

