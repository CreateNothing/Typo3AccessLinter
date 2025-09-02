package com.typo3.fluid.linter.inspections;

import org.junit.Test;

import java.util.List;

public class AccordionAccessibilityInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldOfferFixes_whenAccordionButtonMissingAriaAttributes() {
        String html = wrapBody(
            "<div class='accordion'>" +
                "<button class='accordion'>Section 1</button>" +
                "<div class='panel' id='p1'></div>" +
            "</div>"
        );

        var highlights = highlight("test.html", html, new AccordionAccessibilityInspection());
        // Collect all quick-fix texts
        List<String> fixes = quickFixTexts();

        // Expect fixes to add aria-expanded and aria-controls
        assertFixesContain(fixes,
                "Add aria-expanded attribute",
                "Add aria-controls attribute");
    }
}
