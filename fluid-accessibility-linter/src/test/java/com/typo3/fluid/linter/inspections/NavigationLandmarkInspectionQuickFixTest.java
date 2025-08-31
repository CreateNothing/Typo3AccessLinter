package com.typo3.fluid.linter.inspections;

import org.junit.Test;

import java.util.List;

public class NavigationLandmarkInspectionQuickFixTest extends BaseInspectionTest {

    @Test
    public void testShouldOfferFix_whenNavHasRedundantRole() {
        String html = wrapBody("<nav role='navigation'><a href='#'>Home</a></nav>");

        var highlights = highlight("test.html", html, new NavigationLandmarkInspection());
        List<String> fixes = quickFixTexts();

        assertFixesContain(fixes, "Remove redundant role attribute");
    }
}

