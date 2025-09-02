package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class LiveRegionInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenInvalidAriaLiveValue() {
        String html = "<div aria-live=\"loud\"></div>";
        var highlights = highlight(html, new LiveRegionInspection());
        assertHighlightsContain(highlights, "Invalid aria-live value 'loud'");
    }

    @Test
    public void testShouldWarn_whenAriaAtomicInvalid() {
        String html = "<div aria-atomic=\"maybe\"></div>";
        var highlights = highlight(html, new LiveRegionInspection());
        assertHighlightsContain(highlights, "aria-atomic must be 'true' or 'false'");
    }

    @Test
    public void testShouldWarn_whenAriaRelevantInvalid() {
        String html = "<div aria-relevant=\"adds\"></div>";
        var highlights = highlight(html, new LiveRegionInspection());
        assertHighlightsContain(highlights, "Invalid aria-relevant value 'adds'");
    }

    @Test
    public void testShouldInfo_whenRedundantAriaLiveWithImplicitRoleStatus() {
        String html = "<div role=\"status\" aria-live=\"polite\"></div>";
        var highlights = highlight(html, new LiveRegionInspection());
        assertHighlightsContain(highlights, "Redundant aria-live with role='status'");
    }
}
