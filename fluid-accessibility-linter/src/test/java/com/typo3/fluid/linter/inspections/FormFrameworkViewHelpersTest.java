package com.typo3.fluid.linter.inspections;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Placeholder tests for TYPO3 Form Framework ViewHelpers (formvh:*).
 * Marked @Ignore until the linter inspects formvh outputs (datePicker, timePicker, etc.).
 */
public class FormFrameworkViewHelpersTest extends BaseInspectionTest {

    @Ignore("Pending: add Form Framework element analysis")
    @Test
    public void testFormvhFormControlsShouldHaveLabels() {
        String html = "<formvh:form><input type=\"text\"></formvh:form>";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "missing label");
    }

    @Ignore("Pending: add Form Framework element analysis")
    @Test
    public void testDatePickerShouldBeKeyboardAccessible() {
        String html = "<formvh:form.datePicker />";
        var highlights = highlight(html, new AriaRoleInspection());
        assertNoHighlightsContaining(highlights, "keyboard inaccessible");
    }

    @Ignore("Pending: add Form Framework element analysis")
    @Test
    public void testUploadedResourceShouldAnnounceFileName() {
        String html = "<formvh:uploadedResource />";
        var highlights = highlight(html, new LiveRegionInspection());
        assertNoHighlightsContaining(highlights, "not announced");
    }
}

