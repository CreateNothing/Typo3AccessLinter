package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.junit.Test;

import java.util.List;

public class ModalDialogInspectionQuickFixTest extends BaseInspectionTest {

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
    public void testShouldAddAriaModal_onDialogMissing() {
        String html = wrapBody("<div role=\"dialog\">Content</div>");
        highlight("test.html", html, new ModalDialogInspection());

        applyFixContaining("Add aria-modal='true' to dialog");

        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("aria-modal=\"true\""));
    }

    @Test
    public void testShouldFixAriaModalValue_toTrue() {
        String html = wrapBody("<div role=\"dialog\" aria-modal=\"foo\">X</div>");
        highlight("test.html", html, new ModalDialogInspection());

        applyFixContaining("Change aria-modal to 'true'");

        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("aria-modal=\"true\""));
        assertFalse(updated.contains("aria-modal=\"foo\""));
    }

    @Test
    public void testShouldAddAriaLabel_onDialogMissingLabel() {
        String html = wrapBody("<div role=\"dialog\">Dialog</div>");
        highlight("test.html", html, new ModalDialogInspection());

        applyFixContaining("Add aria-label to dialog");

        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("aria-label=\"Dialog\"") || updated.contains("aria-label=\"Modal\""));
    }

    @Test
    public void testShouldFixPositiveTabindex_toMinusOne() {
        String html = wrapBody("<div role=\"dialog\" tabindex=\"2\">X</div>");
        highlight("test.html", html, new ModalDialogInspection());

        applyFixContaining("Fix dialog tabindex value");

        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("tabindex=\"-1\""));
        assertFalse(updated.contains("tabindex=\"2\""));
    }

    @Test
    public void testShouldAddRole_onModalLikeDiv() {
        String html = wrapBody("<div class=\"modal\">X</div>");
        highlight("test.html", html, new ModalDialogInspection());

        applyFixContaining("Add role='dialog' to modal element");

        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("role=\"dialog\""));
    }

    @Test
    public void testShouldMakeDialogProgrammaticallyFocusable_whenNoFocusableChildren() {
        String html = wrapBody("<div role=\"dialog\">Plain</div>");
        highlight("test.html", html, new ModalDialogInspection());

        applyFixContaining("Make dialog programmatically focusable");

        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("tabindex=\"-1\""));
    }
}

