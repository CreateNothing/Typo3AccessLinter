package com.typo3.fluid.linter.strategy;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.typo3.fluid.linter.rules.RuleEngine;
import org.junit.Test;

public class HeadingHierarchyValidationStrategyTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    @Test
    public void testShouldFlagJumpFromH1ToH3() {
        String html = "<html><body><h1>T</h1><h3>Skip</h3></body></html>";
        PsiFile file = myFixture.configureByText("head.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean found = violations.stream().anyMatch(v -> v.getMessage().contains("h1 to h3"));
        assertTrue("Expected heading level jump warning", found);
    }

    @Test
    public void testShouldPassSequentialHeadings() {
        String html = "<html><body><h1>A</h1><h2>B</h2><h2>C</h2><h3>D</h3></body></html>";
        PsiFile file = myFixture.configureByText("head-ok.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean anyJump = violations.stream().anyMatch(v -> v.getMessage().contains("Heading level jumps"));
        assertFalse("Sequential headings should not warn", anyJump);
    }
}

