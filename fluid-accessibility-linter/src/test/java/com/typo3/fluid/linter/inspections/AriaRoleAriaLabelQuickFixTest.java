package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.junit.Test;

import java.util.List;

public class AriaRoleAriaLabelQuickFixTest extends BaseInspectionTest {

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
    public void testShouldRemoveRedundantRoleWord_fromAriaLabel() {
        String html = wrapBody("<div role=\"button\" aria-label=\"Button Submit\">Submit</div>");
        highlight("test.html", html, new AriaRoleInspection());

        applyFixContaining("Remove redundant 'button' from aria-label");

        String updated = myFixture.getFile().getText();
        // aria-label should now be just "Submit"
        assertTrue(updated.contains("aria-label=\"Submit\""));
    }

    @Test
    public void testShouldRemoveUnnecessaryAriaLabel_whenDuplicatesVisibleText() {
        String html = wrapBody("<button aria-label=\"Save\">Save</button>");
        highlight("test.html", html, new AriaRoleInspection());

        applyFixContaining("Remove unnecessary aria-label");

        String updated = myFixture.getFile().getText();
        assertFalse(updated.contains("aria-label=\"Save\""));
    }

    @Test
    public void testShouldShortenAriaLabel_andAddDescribedby() {
        String longLabel = "Click the button to proceed to the next step and confirm all your settings including notifications, privacy preferences, and account details in a single action.";
        String html = wrapBody("<button aria-label=\"" + longLabel + "\">Go</button>");
        highlight("test.html", html, new AriaRoleInspection());

        applyFixContaining("Shorten aria-label and move details to aria-describedby");

        String updated = myFixture.getFile().getText();
        // Should be shortened and include aria-describedby placeholder
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("aria-label=\\\"([^\\\"]*)\\\"").matcher(updated);
        assertTrue("aria-label not found after fix", m.find());
        String newValue = m.group(1);
        assertTrue("aria-label too long after fix: " + newValue.length(), newValue.length() <= 100);
        assertTrue("aria-describedby missing", updated.contains("aria-describedby=\"details-id\""));
    }
}
