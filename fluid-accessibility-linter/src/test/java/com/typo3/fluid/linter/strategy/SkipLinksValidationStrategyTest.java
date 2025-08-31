package com.typo3.fluid.linter.strategy;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.typo3.fluid.linter.rules.RuleEngine;
import org.junit.Test;

import java.util.stream.Collectors;

public class SkipLinksValidationStrategyTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    @Test
    public void testShouldReportMissingSkipLink_whenNonePresent() {
        String html = "<html><body><nav>Menu</nav><main id=\"main\">Content</main></body></html>";
        PsiFile file = myFixture.configureByText("page.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        boolean found = violations.stream().anyMatch(v -> v.getMessage().toLowerCase().contains("skip"));
        assertTrue("Expected a skip link suggestion", found);
    }

    @Test
    public void testShouldReportBrokenSkipLink_whenTargetMissing() {
        String html = "<html><body><a href=\"#main\">Skip to content</a><div>no main</div></body></html>";
        PsiFile file = myFixture.configureByText("page.html", html).getContainingFile();
        var violations = RuleEngine.getInstance().execute(file);
        String msgs = violations.stream().map(v -> v.getMessage()).collect(Collectors.joining("\n"));
        assertTrue(msgs, msgs.contains("target '#main'"));
    }
}
