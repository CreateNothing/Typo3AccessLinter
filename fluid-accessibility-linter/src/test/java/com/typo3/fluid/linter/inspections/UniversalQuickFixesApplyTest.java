package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

import java.util.List;

public class UniversalQuickFixesApplyTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    @Test
    public void testApplyAddAriaLabel_toInput() {
        String html = "<form><input type=\"text\"></form>";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();

        // Apply: Add aria-label to input
        applyFixContaining("Add aria-label to input");

        String updated = myFixture.getFile().getText();
        assertTrue("Expected aria-label to be added", updated.contains("aria-label=\""));
    }

    @Test
    public void testApplyAddSkipLink_atTop() {
        String html = "<html><body><main></main></body></html>";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();

        // Apply: Add skip link at top
        applyFixContaining("Add skip link at top");

        String updated = myFixture.getFile().getText();
        assertTrue("Expected skip link to be inserted",
                updated.contains("Skip to main content"));
    }

    private void enableUniversal() {
        myFixture.enableInspections(new UniversalAccessibilityInspection());
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
    public void testApplyAddAltAttribute_onMissingAlt() {
        String html = "<img src=\"photo.jpg\">";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();

        // Apply: Add alt attribute
        applyFixContaining("Add alt attribute");

        String updated = myFixture.getFile().getText();
        assertTrue("Expected alt attribute to be added", updated.contains("alt=\"\""));
    }

    @Test
    public void testApplyGenerateTableHeaders() {
        String html = "<table><tr><td>A</td><td>B</td></tr></table>";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();

        // Apply: Generate <thead> with <th> headers
        applyFixContaining("Generate <thead> with <th> headers");

        String updated = myFixture.getFile().getText();
        assertTrue("Expected thead to be generated",
                updated.contains("<thead><tr><th scope=\"col\">Header 1</th><th scope=\"col\">Header 2</th></tr></thead>"));
    }
}
