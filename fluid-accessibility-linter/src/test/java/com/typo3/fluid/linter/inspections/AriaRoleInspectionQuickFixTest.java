package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;

import java.util.List;

public class AriaRoleInspectionQuickFixTest extends BaseInspectionTest {

    public void testShouldRemoveRedundantRole_onNav() {
        String html = wrapBody("<nav role=\"navigation\">Menu</nav>");
        highlight("test.html", html, new AriaRoleInspection());

        applyFixContaining("Remove redundant ARIA role");

        String updated = myFixture.getFile().getText();
        assertFalse(updated.contains(" role=\"navigation\""));
    }

    public void testShouldRemoveInvalidRole_attribute() {
        String html = wrapBody("<div role=\"notarole\">X</div>");
        highlight("test.html", html, new AriaRoleInspection());

        // Expect a fix to remove invalid role
        applyFixContaining("Remove invalid ARIA role");

        String updated = myFixture.getFile().getText();
        assertFalse(updated.contains(" role=\"notarole\""));
    }

    public void testShouldRemoveAriaHidden_onInteractive() {
        String html = wrapBody("<button aria-hidden=\"true\">Hidden</button>");
        highlight("test.html", html, new AriaRoleInspection());

        applyFixContaining("Remove aria-hidden from interactive element");

        String updated = myFixture.getFile().getText();
        assertFalse(updated.contains("aria-hidden=\"true\""));
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
}
