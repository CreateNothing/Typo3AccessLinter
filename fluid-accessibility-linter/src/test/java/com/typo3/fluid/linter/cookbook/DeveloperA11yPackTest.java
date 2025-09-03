package com.typo3.fluid.linter.cookbook;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.typo3.fluid.linter.rules.RuleEngine;
import com.typo3.fluid.linter.rules.RuleViolation;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DeveloperA11yPackTest extends LightJavaCodeInsightFixtureTestCase {

    static class Pack {
        Meta meta;
        List<TestItem> tests;
    }

    static class Meta { String title; String version; String license; List<String> scope_excludes; }

    static class TestItem {
        String id; List<String> wcag; String purpose;
        @SerializedName("html_ok") String htmlOk;
        @SerializedName("html_bad") String htmlBad;
        @SerializedName("fluid_ok") String fluidOk;
        @SerializedName("fluid_bad") String fluidBad;
        @SerializedName("how_to_test") List<String> howToTest;
        String automatable; List<String> links; List<String> tags;
    }

    @Override protected String getTestDataPath() { return "src/test/resources/testData"; }

    private Pack loadPack() throws IOException {
        String json = Files.readString(Paths.get(getTestDataPath(), "developer-a11y-pack.json"), StandardCharsets.UTF_8);
        return new Gson().fromJson(json, Pack.class);
    }

    private List<RuleViolation> analyze(String name, String markup) {
        PsiFile file = myFixture.configureByText(name, wrap(markup)).getContainingFile();
        return new ArrayList<>(RuleEngine.getInstance().execute(file));
    }

    private String wrap(String snippet) {
        if (snippet == null) return "<html><body></body></html>";
        String low = snippet.toLowerCase();
        if (low.contains("<html") || low.contains("<head") || low.contains("<title")) {
            return snippet; // already a full document
        }
        return "<html><body>" + snippet + "</body></html>";
    }
    private List<String> messages(List<RuleViolation> v) { return v.stream().map(RuleViolation::getMessage).filter(Objects::nonNull).collect(Collectors.toList()); }

    @Test
    public void testDeveloperA11yPack() throws Exception {
        Pack pack = loadPack();
        assertNotNull(pack.tests);

        for (TestItem t : pack.tests) {
            switch (t.id) {
                case "keyboard-focus-order":
                    assertTabindexCheck(t); break;
                case "focus-visible-and-appearance":
                    assertFocusCssCheck(t); break;
                case "skip-link":
                    assertSkipLinkCheck(t); break;
                case "links-vs-buttons":
                    assertLinkVsButton(t); break;
                case "page-language-and-parts":
                    assertLanguageChecks(t); break;
                case "page-title":
                    assertPageTitle(t); break;
                case "images-alt-text":
                    assertAltText(t); break;
                case "forms-labels-and-grouping":
                    assertFormLabels(t); break;
                case "error-handling-alerts":
                    assertStatusMessages(t); break;
                case "tables-simple-headers":
                    assertSimpleTables(t); break;
                case "reduced-motion-respect":
                    assertReducedMotion(t); break;
                case "target-size-minimum":
                    assertTargetSize(t); break;
                case "meaningful-control-text":
                    assertMeaningfulLinkText(t); break;
                // non-automated or out-of-scope for this linter runtime:
                default:
                    // Skip
            }
        }
    }

    private void assertTabindexCheck(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK should not flag positive tabindex", anyContains(messages(ok), "tabindex"));
        assertTrue("BAD should flag positive tabindex", anyContains(messages(bad), "tabindex"));
    }

    private void assertFocusCssCheck(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK should not flag focus outline removal", anyContains(messages(ok), "focus"));
        assertTrue("BAD should flag removed focus outline", anyContains(messages(bad), "outline"));
    }

    private void assertSkipLinkCheck(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK skip link should not be missing", anyContains(messages(ok), "skip navigation"));
        assertTrue("BAD should complain about missing skip link", anyContains(messages(bad), "skip navigation"));
    }

    private void assertLinkVsButton(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK should not flag link/button semantics", anyContains(messages(ok), "Anchor used as button") || anyContains(messages(ok), "Button used for navigation"));
        assertTrue("BAD should flag link/button misuse", anyContains(messages(bad), "Anchor used as button") || anyContains(messages(bad), "Button used for navigation"));
    }

    private void assertLanguageChecks(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK should not flag language issues", anyContains(messages(ok), "lang"));
        assertTrue("BAD should flag language issues", anyContains(messages(bad), "lang") || anyContains(messages(bad), "language"));
    }

    private void assertPageTitle(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertTrue("BAD should flag missing or generic title", anyContains(messages(bad), "title"));
    }

    private void assertAltText(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertTrue("BAD images alt should be flagged", anyContains(messages(bad), "alt"));
    }

    private void assertFormLabels(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertTrue("BAD form without label should be flagged", anyContains(messages(bad), "label"));
        // OK should not have missing-label warnings
        assertFalse("OK form should not have missing-label warnings", anyContains(messages(ok), "missing id attribute and label") || anyContains(messages(ok), "missing associated label"));
    }

    private void assertStatusMessages(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK with role=alert should pass", anyContains(messages(ok), "alert"));
        assertTrue("BAD hidden error without alert should be flagged", anyContains(messages(bad), "Status messages"));
    }

    private void assertSimpleTables(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        // rely on existing table headers strategy
        assertTrue("BAD table without headers should be flagged", messages(bad).size() > 0);
    }

    private void assertReducedMotion(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK reduced motion should pass", anyContains(messages(ok), "reduced-motion"));
        assertTrue("BAD animations without prefers-reduced-motion should be flagged", anyContains(messages(bad), "reduced-motion") || anyContains(messages(bad), "motion"));
    }

    private void assertTargetSize(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK target size should pass", anyContains(messages(ok), "24x24"));
        assertTrue("BAD tiny target should be flagged", anyContains(messages(bad), "24x24"));
    }

    private void assertMeaningfulLinkText(TestItem t) {
        var ok = analyze(t.id+"-ok.html", t.htmlOk);
        var bad = analyze(t.id+"-bad.html", t.htmlBad);
        assertFalse("OK links descriptive", anyContains(messages(ok), "not descriptive"));
        assertTrue("BAD 'Click here' should be flagged", anyContains(messages(bad), "not descriptive") || anyContains(messages(bad), "needs context"));
    }

    private boolean anyContains(List<String> msgs, String needle) {
        String n = needle.toLowerCase();
        return msgs.stream().map(String::toLowerCase).anyMatch(m -> m.contains(n));
    }
}
