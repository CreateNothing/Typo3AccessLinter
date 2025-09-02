package com.typo3.fluid.linter.inspections;

import org.junit.Test;

import java.util.List;

public class AriaLabelValidationInspectionQuickFixTest extends BaseInspectionTest {

    @Test
    public void testShouldOfferFix_whenIconOnlyButtonLacksAccessibleName() {
        String html = wrapBody("<button>✎</button>");

        var highlights = highlight("test.html", html, new AriaLabelValidationInspection());
        List<String> fixes = quickFixTexts();

        assertFixesContain(fixes, "Add aria-label for accessibility");
    }
}
