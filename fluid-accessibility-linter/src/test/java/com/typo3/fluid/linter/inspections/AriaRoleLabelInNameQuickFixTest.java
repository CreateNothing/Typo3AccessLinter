package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.junit.Test;

import java.util.List;

public class AriaRoleLabelInNameQuickFixTest extends BaseInspectionTest {

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
    public void testShouldPrependVisibleText_toAriaLabel() {
        String html = wrapBody("<a href=\"http://www.google.com\" aria-label=\"Going to School\">Google</a>");
        highlight("test.html", html, new AriaRoleInspection());

        // The friendly message appears; quick-fix name uses IncludeVisibleTextInAriaLabelFix family name
        applyFixContaining("Include visible text");

        String updated = myFixture.getFile().getText();
        // aria-label should now begin with Google
        assertTrue(updated.contains("aria-label=\"Google — Going to School\""));
    }
}

