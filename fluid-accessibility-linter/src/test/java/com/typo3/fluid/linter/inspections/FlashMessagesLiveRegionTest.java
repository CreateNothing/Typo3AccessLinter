package com.typo3.fluid.linter.inspections;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Placeholder tests for f:flashMessages announcements.
 * TODO: Add inspection to encourage role="status" or aria-live on containers
 *       wrapping flash messages for SR announcement.
 */
public class FlashMessagesLiveRegionTest extends BaseInspectionTest {

    @Ignore("Pending: implement flash messages live region guidance")
    @Test
    public void testFlashMessagesShouldBeInLiveRegion() {
        String html = "<f:flashMessages />";
        var highlights = highlight(html, new LiveRegionInspection());
        // Expect a recommendation once implemented
        assertHighlightsContain(highlights, "Flash messages should be announced via aria-live or role='status'");
    }

    @Ignore("Pending: implement flash messages live region guidance")
    @Test
    public void testFlashMessagesInsideStatusRegionIsOk() {
        String html = "<div role=\"status\"><f:flashMessages /></div>";
        var highlights = highlight(html, new LiveRegionInspection());
        assertNoHighlightsContaining(highlights, "Flash messages should be announced");
    }
}

