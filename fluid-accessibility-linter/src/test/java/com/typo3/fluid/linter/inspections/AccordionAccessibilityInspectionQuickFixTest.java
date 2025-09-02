package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.junit.Test;

import java.util.List;

public class AccordionAccessibilityInspectionQuickFixTest extends BaseInspectionTest {

    @Test
    public void testAddAriaExpanded_onTriggerMissing() {
        String html = wrapBody("<div class=\"accordion\"><button class=\"accordion-toggle\">Section</button></div>");
        highlight("test.html", html, new AccordionAccessibilityInspection());
        applyFixContaining("Add aria-expanded attribute");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("aria-expanded=\"false\""));
    }

    @Test
    public void testNormalizeAriaExpanded_invalidValue() {
        String html = wrapBody("<div class=\"accordion\"><button class=\"accordion-toggle\" aria-expanded=\"maybe\">X</button></div>");
        highlight("test.html", html, new AccordionAccessibilityInspection());
        applyFixContaining("Fix aria-expanded value");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("aria-expanded=\"false\""));
    }

    @Test
    public void testAddAriaControls_andPanelId() {
        String html = wrapBody("<div class=\"accordion\">\n" +
                "  <button class=\"accordion-toggle\">Title</button>\n" +
                "  <div class=\"panel\">Content</div>\n" +
                "</div>");
        highlight("test.html", html, new AccordionAccessibilityInspection());
        applyFixContaining("Add id to accordion panel");
        applyFixContaining("Add aria-controls attribute");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("aria-controls=\"acc-panel-"));
        assertTrue(updated.contains(" id=\"acc-panel-"));
    }

    @Test
    public void testAddRole_onLinkTrigger() {
        String html = wrapBody("<div class=\"accordion\"><a class=\"accordion-toggle\">Toggle</a></div>");
        highlight("test.html", html, new AccordionAccessibilityInspection());
        applyFixContaining("Add role='button' to accordion trigger");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("role=\"button\""));
    }

    // Tabindex quick-fix is optional in this inspection and may be offered via broader focus management.

    private void applyFixContaining(String needle) {
        List<IntentionAction> fixes = myFixture.getAllQuickFixes();
        IntentionAction target = fixes.stream()
                .filter(a -> a.getText() != null && a.getText().contains(needle))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Quick-fix not found containing: " + needle +
                        "\nAvailable: " + fixes.stream().map(IntentionAction::getText).toList()));
        myFixture.launchAction(target);
    }
}
