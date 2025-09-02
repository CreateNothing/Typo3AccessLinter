package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
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

    @Test
    public void testShouldAddAriaSelected() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1' aria-controls='p1'>Tab 1</div>" +
              "<div role='tabpanel' id='p1'></div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Add aria-selected attribute");

        String updated = myFixture.getFile().getText();
        boolean hasAriaSelected = updated.matches("(?s).*<[^>]*role=['\"]tab['\"][^>]*aria-selected=\\\"false\\\"[^>]*>.*");
        assertTrue("Expected aria-selected=\"false\" to be added to the tab", hasAriaSelected);
    }

    @Test
    public void testShouldNormalizeAriaSelected() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1' aria-selected='maybe' aria-controls='p1'>Tab 1</div>" +
              "<div role='tabpanel' id='p1'></div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Fix aria-selected value");

        String updated = myFixture.getFile().getText();
        assertTrue("Expected aria-selected to be normalized to false",
                updated.contains("aria-selected=\"false\""));
        assertFalse("Should not keep invalid aria-selected value",
                updated.contains("aria-selected=\"maybe\""));
    }

    private void applyFixContaining(String needle) {
        List<IntentionAction> fixes = myFixture.getAllQuickFixes();
        IntentionAction target = fixes.stream()
                .filter(a -> a.getText() != null && a.getText().contains(needle))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Quick-fix not found containing: " + needle +
                        "\nAvailable: " + fixes.stream().map(IntentionAction::getText).toList()));
        myFixture.launchAction(target);
    }

    @Test
    public void testShouldAddAriaControls() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1'>Tab 1</div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Add aria-controls to tab");

        String updated = myFixture.getFile().getText();
        boolean hasAriaControls = updated.matches("(?s).*<[^>]*role=['\"]tab['\"][^>]*aria-controls=\\\"[^\"]+\\\"[^>]*>.*");
        assertTrue("Expected aria-controls to be added to the tab", hasAriaControls);
    }

    @Test
    public void testShouldAddPanelId() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1' aria-controls='p1'>Tab 1</div>" +
              "<div role='tabpanel'></div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Add id to tabpanel");

        String updated = myFixture.getFile().getText();
        boolean hasPanelId = updated.matches("(?s).*<[^>]*role=['\"]tabpanel['\"][^>]*id=\\\"[^\"]+\\\"[^>]*>.*");
        assertTrue("Expected id attribute to be added to tabpanel", hasPanelId);
    }

    @Test
    public void testShouldLinkPanelToTab() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1' aria-controls='p1'>Tab 1</div>" +
              "<div role='tabpanel' id='p1'></div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Add aria-labelledby to tabpanel");

        String updated = myFixture.getFile().getText();
        boolean hasLabelledBy = updated.matches("(?s).*<[^>]*role=['\"]tabpanel['\"][^>]*id=['\"]p1['\"][^>]*aria-labelledby=['\"][^'\"]+['\"][^>]*>.*");
        assertTrue("Expected tabpanel to have aria-labelledby referencing a tab id", hasLabelledBy);
    }

    @Test
    public void testShouldLabelTablist() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1' aria-controls='p1'>Tab 1</div>" +
              "<div role='tabpanel' id='p1' aria-labelledby='t1'></div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Add aria-label to tablist");

        String updated = myFixture.getFile().getText();
        assertTrue("Expected aria-label=\"Tabs\" to be added to tablist",
                updated.contains("aria-label=\"Tabs\""));
    }

    @Test
    public void testShouldNormalizeOrientation() {
        String html = wrapBody(
            "<div role='tablist' aria-orientation=\"sideways\">" +
              "<div role='tab' id='t1' aria-controls='p1' aria-selected=\"false\">Tab 1</div>" +
              "<div role='tabpanel' id='p1' aria-labelledby='t1' tabindex='0'></div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Fix aria-orientation value");

        String updated = myFixture.getFile().getText();
        assertTrue("Expected aria-orientation to normalize to horizontal",
                updated.contains("aria-orientation=\"horizontal\""));
    }

    @Test
    public void testShouldFixTabindexSelected() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1' aria-controls='p1' aria-selected='true' tabindex='-1'>Tab 1</div>" +
              "<div role='tabpanel' id='p1' aria-labelledby='t1' tabindex='0'></div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Fix tabindex consistency in tablist");

        String updated = myFixture.getFile().getText();
        boolean ok = updated.matches("(?s).*<[^>]*role=['\"]tab['\"][^>]*aria-selected=['\"]true['\"][^>]*tabindex=\\\"0\\\"[^>]*>.*");
        assertTrue("Expected selected tab to have tabindex=0", ok);
    }

    @Test
    public void testShouldFixTabindexInactive() {
        String html = wrapBody(
            "<div role='tablist'>" +
              "<div role='tab' id='t1' aria-controls='p1' aria-selected='false' tabindex='0'>Tab 1</div>" +
              "<div role='tabpanel' id='p1' aria-labelledby='t1' tabindex='0'></div>" +
            "</div>"
        );

        highlight("test.html", html, new TabPanelInspection());
        applyFixContaining("Change inactive tab tabindex to '-1'");

        String updated = myFixture.getFile().getText();
        boolean ok = updated.matches("(?s).*<[^>]*role=['\"]tab['\"][^>]*aria-selected=['\"]false['\"][^>]*tabindex=\\\"-1\\\"[^>]*>.*");
        assertTrue("Expected inactive tab to have tabindex=-1", ok);
    }
}
