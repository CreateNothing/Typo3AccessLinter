package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;

import java.util.List;

public class LiveRegionInspectionQuickFixTest extends BaseInspectionTest {

    public void testFixInvalidAriaLive_toPolite() {
        String html = wrapBody("<div aria-live=\"loud\">X</div>");
        highlight("test.html", html, new LiveRegionInspection());
        applyFixContaining("Fix aria-live value");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("aria-live=\"polite\""));
    }

    public void testRemoveRedundantAriaLive_withStatusRole() {
        String html = wrapBody("<div role=\"status\" aria-live=\"polite\">X</div>");
        highlight("test.html", html, new LiveRegionInspection());
        applyFixContaining("Remove redundant aria-live");
        String updated = myFixture.getFile().getText();
        assertFalse(updated.contains("aria-live"));
    }

    public void testAddStatusRole_forMessage() {
        String html = wrapBody("<div class=\"message info\">Saved</div>");
        highlight("test.html", html, new LiveRegionInspection());
        applyFixContaining("Add role='status' for screen reader announcement");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("role=\"status\""));
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
