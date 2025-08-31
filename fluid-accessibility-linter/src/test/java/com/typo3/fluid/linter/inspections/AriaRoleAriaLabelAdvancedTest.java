package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class AriaRoleAriaLabelAdvancedTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenAriaLabelledbyReferencesMissingId() {
        String html = wrapBody("<div role=\"dialog\" aria-labelledby=\"missingId\">X</div>");
        var highlights = highlight(html, new AriaRoleInspection());
        assertHighlightsContain(highlights, "aria-labelledby references non-existent ID: 'missingId'");
    }

    @Test
    public void testShouldWarn_whenAriaLabelContainsRedundantRoleWord() {
        String html = wrapBody("<div role=\"button\" aria-label=\"Button Submit\">Submit</div>");
        var highlights = highlight(html, new AriaRoleInspection());
        assertHighlightsContain(highlights, "Redundant role word in aria-label");
    }

    @Test
    public void testShouldWarn_whenAriaLabelIsTooVerboseOrInstructional() {
        String longLabel = "Click the button to proceed to the next step and confirm all your settings including notifications, privacy preferences, and account details in a single action.";
        String html = wrapBody("<button aria-label=\"" + longLabel + "\">Go</button>");
        var highlights = highlight(html, new AriaRoleInspection());
        assertHighlightsContain(highlights, "aria-label is too verbose", "Instructions should use aria-describedby");
    }

    @Test
    public void testShouldWarn_whenAriaLabelDuplicatesVisibleText() {
        String html = wrapBody("<button aria-label=\"Save\">Save</button>");
        var highlights = highlight(html, new AriaRoleInspection());
        assertHighlightsContain(highlights, "Unnecessary aria-label that duplicates visible text");
    }
}

