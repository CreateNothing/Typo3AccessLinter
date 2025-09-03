package com.typo3.fluid.linter.strategy;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.typo3.fluid.linter.rules.RuleEngine;
import org.junit.Test;

public class FormLabelValidationStrategyTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    @Test
    public void testShouldPassWhenLabelForMatchesId() {
        String html = "<html><body><label for=\"email\">Email</label><input id=\"email\" type=\"email\"></body></html>";
        PsiFile file = myFixture.configureByText("form.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean any = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("label"));
        assertFalse("Labeled input should pass", any);
    }

    @Test
    public void testShouldPassWhenAriaLabelProvided() {
        String html = "<html><body><input type=\"text\" aria-label=\"Search\"></body></html>";
        PsiFile file = myFixture.configureByText("form-aria.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean any = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("label"));
        assertFalse("aria-label should satisfy labeling", any);
    }

    @Test
    public void testShouldFlagWhenNoIdAndNoAria() {
        String html = "<html><body><input type=\"text\"></body></html>";
        PsiFile file = myFixture.configureByText("form-missing.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean found = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("missing id"));
        assertTrue("Missing id and label should be flagged", found);
    }

    @Test
    public void testShouldFlagWhenIdButNoLabel() {
        String html = "<html><body><input id=\"x\" type=\"text\"></body></html>";
        PsiFile file = myFixture.configureByText("form-nolabel.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean found = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("missing associated label"));
        assertTrue("ID without matching label should be flagged", found);
    }

    @Test
    public void testShouldPassWhenAriaLabelledbyMatchesLabelId() {
        String html = "<html><body>" +
                "<label id=\"label1\">First Name</label>" +
                "<input type=\"text\" aria-labelledby=\"label1\">" +
                "</body></html>";
        PsiFile file = myFixture.configureByText("form-aria-labelledby-ok.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean anyLabelIssue = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("label"));
        assertFalse("aria-labelledby should satisfy labeling when id exists", anyLabelIssue);
    }

    @Test
    public void testShouldFailWhenAriaLabelledbyReferencesMissingId() {
        String html = "<html><body>" +
                "<label id=\"label1\">First Name</label>" +
                "<input type=\"text\" aria-labelledby=\"doesNotExist\">" +
                "</body></html>";
        PsiFile file = myFixture.configureByText("form-aria-labelledby-missing.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean found = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("aria-labelledby references non-existent"));
        assertTrue("Missing aria-labelledby idref should be flagged", found);
    }
}
