package com.typo3.fluid.linter.strategy;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.typo3.fluid.linter.rules.RuleEngine;
import org.junit.Test;

public class TableAccessibilityValidationStrategyTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    @Test
    public void testShouldFlagTableWithoutHeaders_whenNoThPresent() {
        String html = "<html><body><table><tr><td>A</td><td>B</td></tr></table></body></html>";
        PsiFile file = myFixture.configureByText("table.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean found = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("header"));
        assertTrue("Expected missing table headers warning", found);
    }

    @Test
    public void testShouldNotFlagLayoutTable_whenRolePresentation() {
        String html = "<html><body><table role=\"presentation\"><tr><td>A</td></tr></table></body></html>";
        PsiFile file = myFixture.configureByText("table.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean anyTableHeaderViolation = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("header"));
        assertFalse("Layout table should not be flagged", anyTableHeaderViolation);
    }

    @Test
    public void testShouldPassTableWithTh_whenHeadersPresent() {
        String html = "<html><body><table><tr><th>H</th><th>H2</th></tr><tr><td>A</td><td>B</td></tr></table></body></html>";
        PsiFile file = myFixture.configureByText("table.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean anyTableHeaderViolation = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("header"));
        assertFalse("Table with TH should not be flagged", anyTableHeaderViolation);
    }
}
