package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class FluidImageViewHelperAltTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenFluidImageMissingAlt() {
        String html = "<f:image src=\"photo.jpg\" />";
        var highlights = highlight(html, new MissingAltTextInspection());
        assertHighlightsContain(highlights, "missing alt attribute");
    }

    @Test
    public void testShouldNotWarn_whenFluidImageHasAlt() {
        String html = "<f:image src=\"photo.jpg\" alt=\"Beautiful landscape\" />";
        var highlights = highlight(html, new MissingAltTextInspection());
        assertNoHighlightsContaining(highlights, "missing alt attribute");
    }

    @Test
    public void testShouldNotWarn_whenFluidImageIsDecorative() {
        String html = "<f:image src=\"decorative.jpg\" aria-hidden=\"true\" />";
        var highlights = highlight(html, new MissingAltTextInspection());
        assertNoHighlightsContaining(highlights, "missing alt attribute");
    }

    @Test
    public void testShouldWarn_whenNamespacedImageMissingAlt() {
        // Custom namespace with :image suffix should be treated like an image ViewHelper
        String html = "<my:image src=\"x.jpg\" />";
        var highlights = highlight(html, new MissingAltTextInspection());
        assertHighlightsContain(highlights, "missing alt attribute");
    }

    @Test
    public void testShouldDetectFluidImageInsideControlFlow() {
        String html = """
            <f:if condition=\"{show}\"> 
                <f:then>
                    <f:image src=\"a.jpg\" />
                </f:then>
            </f:if>
            """;
        var highlights = highlight(html, new MissingAltTextInspection());
        assertHighlightsContain(highlights, "missing alt attribute");
    }

    @Test
    public void testShouldWarn_whenFluidMediaMissingAlt() {
        String html = "<f:media src=\"file.jpg\" />";
        var highlights = highlight(html, new MissingAltTextInspection());
        assertHighlightsContain(highlights, "missing alt attribute");
    }

    @Test
    public void testShouldWarn_whenVhsMediaImageMissingAlt() {
        String html = "<v:media.image src=\"file.jpg\" />";
        var highlights = highlight(html, new MissingAltTextInspection());
        assertHighlightsContain(highlights, "missing alt attribute");
    }

    @Test
    public void testShouldWarn_whenTypo3NamespacedMediaMissingAlt() {
        String html = "<typo3:media src=\"file.jpg\" />";
        var highlights = highlight(html, new MissingAltTextInspection());
        assertHighlightsContain(highlights, "missing alt attribute");
    }
}
