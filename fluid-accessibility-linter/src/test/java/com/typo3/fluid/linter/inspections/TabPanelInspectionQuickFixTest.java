package com.typo3.fluid.linter.inspections;

import org.junit.Test;

import java.util.List;

public class TabPanelInspectionQuickFixTest extends BaseInspectionTest {

    @Test
    public void testShouldOfferFix_whenTabMissingAriaSelected() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1' aria-controls='p1'>Tab 1</div>" +
              "<div role='tabpanel' id='p1'></div>" +
            "</div>"
        );

        var highlights = highlight("test.html", html, new TabPanelInspection());
        List<String> fixes = quickFixTexts();
        assertFixesContain(fixes, "Add aria-selected attribute");
    }
}
