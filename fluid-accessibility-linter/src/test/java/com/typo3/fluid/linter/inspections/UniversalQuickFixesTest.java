package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class UniversalQuickFixesTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    private List<String> collectFixTexts() {
        List<IntentionAction> actions = myFixture.getAllQuickFixes();
        return actions.stream().map(IntentionAction::getText).collect(Collectors.toList());
    }

    private void enableUniversal() {
        myFixture.enableInspections(new UniversalAccessibilityInspection());
    }

    @Test
    public void testTableHeadersFix() {
        String html = "<table><tr><td>A</td><td>B</td></tr></table>";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();
        List<String> fixes = collectFixTexts();
        assertTrue(fixes.stream().anyMatch(t -> t.contains("Generate <thead> with <th> headers")));
    }

    @Test
    public void testSkipLinkAddFix() {
        String html = "<html><body><main></main></body></html>";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();
        List<String> fixes = collectFixTexts();
        assertTrue(fixes.stream().anyMatch(t -> t.contains("Add skip link at top")));
    }

    @Test
    public void testSkipTargetAddFix() {
        String html = "<a href=\"#main\" class=\"skip\">Skip</a><main></main>";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();
        List<String> fixes = collectFixTexts();
        assertTrue(fixes.stream().anyMatch(t -> t.contains("Add missing skip target id to <main>")));
    }

    @Test
    public void testFormLabelFixes() {
        String html = "<form><input type=\"text\"></form>";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();
        List<String> fixes = collectFixTexts();
        assertTrue(fixes.stream().anyMatch(t -> t.contains("Add <label for> with generated id")));
        assertTrue(fixes.stream().anyMatch(t -> t.contains("Add aria-label to input")));
    }

    @Test
    public void testHeadingLevelFix() {
        String html = "<h1>Title</h1><h3>Sub</h3>";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();
        List<String> fixes = collectFixTexts();
        assertTrue(fixes.stream().anyMatch(t -> t.contains("Change to <h2>")));
    }

    @Test
    public void testImageAltFixes() {
        String html = "<img src=\"photo-banner.png\">";
        myFixture.configureByText("test.html", html);
        enableUniversal();
        myFixture.doHighlighting();
        List<String> fixes = collectFixTexts();
        // from AddAttributeFixStrategy
        assertTrue(fixes.stream().anyMatch(t -> t.equals("Add alt attribute")));
        // additional image fixes
        assertTrue(fixes.stream().anyMatch(t -> t.contains("Mark image as decorative")));
        assertTrue(fixes.stream().anyMatch(t -> t.contains("Set alt from image filename")));
    }
}
