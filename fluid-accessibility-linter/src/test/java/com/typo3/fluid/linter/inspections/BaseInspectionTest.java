package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared helpers for inspection tests.
 */
public abstract class BaseInspectionTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    protected List<HighlightInfo> highlight(String html, LocalInspectionTool... inspections) {
        return highlight("test.html", html, inspections);
    }

    protected List<HighlightInfo> highlight(String filename, String html, LocalInspectionTool... inspections) {
        if (inspections != null && inspections.length > 0) {
            myFixture.enableInspections(inspections);
        }
        myFixture.configureByText(filename, html);
        return new ArrayList<>(myFixture.doHighlighting());
    }

    protected List<String> quickFixTexts() {
        List<IntentionAction> actions = myFixture.getAllQuickFixes();
        if (actions == null) return Collections.emptyList();
        return actions.stream().map(IntentionAction::getText).collect(Collectors.toList());
    }

    protected void assertHighlightsContain(List<HighlightInfo> highlights, String... fragments) {
        for (String fragment : fragments) {
            boolean found = highlights.stream()
                .anyMatch(h -> h.getDescription() != null && h.getDescription().contains(fragment));
            if (!found) {
                System.out.println("Expected fragment not found: " + fragment);
                System.out.println("Available highlights:");
                highlights.forEach(h -> System.out.println("  - " + h.getDescription()));
            }
            assertTrue("Expected highlight not found: " + fragment, found);
        }
    }

    protected void assertNoHighlightsContaining(List<HighlightInfo> highlights, String... fragments) {
        for (String fragment : fragments) {
            boolean any = highlights.stream()
                .anyMatch(h -> h.getDescription() != null && h.getDescription().contains(fragment));
            if (any) {
                System.out.println("Unexpected highlight containing: " + fragment);
                highlights.forEach(h -> System.out.println("  - " + h.getDescription()));
            }
            assertFalse("Unexpected highlight found containing: " + fragment, any);
        }
    }

    protected void assertFixesContain(List<String> fixes, String... fragments) {
        for (String fragment : fragments) {
            boolean found = fixes.stream().anyMatch(text -> text != null && text.contains(fragment));
            if (!found) {
                System.out.println("Expected quick-fix not found: " + fragment);
                fixes.forEach(f -> System.out.println("  - fix: " + f));
            }
            assertTrue("Expected quick-fix not found: " + fragment, found);
        }
    }

    protected void assertNoFixesContaining(List<String> fixes, String... fragments) {
        for (String fragment : fragments) {
            boolean any = fixes.stream().anyMatch(text -> text != null && text.contains(fragment));
            if (any) {
                System.out.println("Unexpected quick-fix containing: " + fragment);
                fixes.forEach(f -> System.out.println("  - fix: " + f));
            }
            assertFalse("Unexpected quick-fix found containing: " + fragment, any);
        }
    }

    // HTML wrappers
    protected String wrapBody(String html) {
        return "<body>" + html + "</body>";
    }

    protected String wrapFull(String bodyHtml) {
        return "<html><head></head><body>" + bodyHtml + "</body></html>";
    }

    protected String addFluidNamespaceToHtml(String fullHtml) {
        // naive injection for tests: add xmlns:f if <html> exists and no namespace present
        if (fullHtml.contains("<html") && !fullHtml.contains("xmlns:f=")) {
            return fullHtml.replaceFirst("<html", "<html xmlns:f=\"http://typo3.org/ns/TYPO3/CMS/Fluid/ViewHelpers\"");
        }
        return fullHtml;
    }

    protected String readTestData(String relativeFile) throws IOException {
        Path p = Paths.get(getTestDataPath(), relativeFile);
        byte[] bytes = Files.readAllBytes(p);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

