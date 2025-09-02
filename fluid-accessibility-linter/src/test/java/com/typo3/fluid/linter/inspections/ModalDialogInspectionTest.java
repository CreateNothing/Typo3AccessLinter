package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class ModalDialogInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenDialogMissingModalAndLabelAndFocusableAndBackdrop() {
        String html = "<div role=\"dialog\"></div>";
        var highlights = highlight(html, new ModalDialogInspection());
        assertHighlightsContain(highlights,
                "Modal dialog should have aria-modal='true'",
                "Dialog must have an accessible name via aria-labelledby or aria-label",
                "Dialog should contain at least one focusable element",
                "Modal should have a backdrop to prevent interaction with background content");
    }

    @Test
    public void testShouldWarn_whenDialogHasInvalidAriaModalValue() {
        String html = "<div role=\"dialog\" aria-modal=\"foo\"></div>";
        var highlights = highlight(html, new ModalDialogInspection());
        assertHighlightsContain(highlights, "Modal dialog should have aria-modal='true', not 'foo'");
    }

    @Test
    public void testShouldWarn_whenModalLikeElementLacksRole() {
        String html = "<div class=\"modal\">X</div>";
        var highlights = highlight(html, new ModalDialogInspection());
        assertHighlightsContain(highlights, "Element appears to be a modal but lacks role='dialog' or role='alertdialog'");
    }

    @Test
    public void testShouldWarn_whenDialogHasPositiveTabindex() {
        String html = "<div role=\"dialog\" tabindex=\"2\"></div>";
        var highlights = highlight(html, new ModalDialogInspection());
        assertHighlightsContain(highlights, "Dialog should not have positive tabindex");
    }
}
