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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JSON-driven tests fed by src/test/resources/testData/aria-usage-cookbook.json
 *
 * These focus on scenarios our current rule engine already covers.
 */
public class AriaUsageCookbookTest extends LightJavaCodeInsightFixtureTestCase {

    // --- JSON model ---
    static class Cookbook {
        Meta meta;
        List<Example> examples;
    }

    static class Meta {
        String title;
        String version;
        String license;
        String disclaimer;
    }

    static class Example {
        String id;
        List<String> attributes;
        String purpose;
        @SerializedName("do")
        List<String> doTips;
        @SerializedName("dont")
        List<String> dontTips;
        @SerializedName("html_ok")
        String htmlOk;
        @SerializedName("html_bad")
        String htmlBad;
        @SerializedName("fluid_ok")
        String fluidOk;
        @SerializedName("fluid_bad")
        String fluidBad;
        List<String> tags;
        List<String> links;
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    private Cookbook loadCookbook() throws IOException {
        String json = Files.readString(Paths.get(getTestDataPath(), "aria-usage-cookbook.json"), StandardCharsets.UTF_8);
        return new Gson().fromJson(json, Cookbook.class);
    }

    private Example getExample(Cookbook cookbook, String id) {
        Optional<Example> ex = cookbook.examples.stream()
                .filter(e -> Objects.equals(e.id, id))
                .findFirst();
        assertTrue("Example not found in cookbook: " + id, ex.isPresent());
        return ex.get();
    }

    private List<RuleViolation> analyze(String name, String markup) {
        PsiFile file = myFixture.configureByText(name, wrap(markup)).getContainingFile();
        return new ArrayList<>(RuleEngine.getInstance().execute(file));
    }

    private String wrap(String snippet) {
        // Ensure a minimal valid HTML container for PSI parsing
        return "<html><body>" + (snippet == null ? "" : snippet) + "</body></html>";
    }

    private List<String> messages(List<RuleViolation> violations) {
        return violations.stream().map(RuleViolation::getMessage).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // --- Targeted coverage where current engine has rules ---

    @Test
    public void testTabs_fromCookbook() throws Exception {
        Cookbook cb = loadCookbook();
        Example ex = getExample(cb, "tabs--apg-structure");

        var okViolations = analyze("tabs-ok.html", ex.htmlOk);
        var badViolations = analyze("tabs-bad.html", ex.htmlBad);

        // OK: should not complain about missing aria-selected on tabs
        assertFalse(
            "OK tabs example should not trigger 'Role \'tab\' requires'",
            messages(okViolations).stream().anyMatch(m -> m.contains("Role 'tab' requires"))
        );

        // BAD: should complain about missing aria-selected on tabs
        assertTrue(
            "BAD tabs example should trigger tab required properties",
            messages(badViolations).stream().anyMatch(m -> m.contains("Role 'tab' requires"))
        );
    }

    @Test
    public void testCombobox_fromCookbook() throws Exception {
        Cookbook cb = loadCookbook();
        Example ex = getExample(cb, "combobox--aria-activedescendant");

        var okViolations = analyze("combobox-ok.html", ex.htmlOk);
        var badViolations = analyze("combobox-bad.html", ex.htmlBad);

        // OK: combobox provides aria-expanded
        assertFalse(
            "OK combobox should not trigger missing aria-expanded",
            messages(okViolations).stream().anyMatch(m -> m.toLowerCase().contains("combobox") && m.toLowerCase().contains("aria-expanded"))
        );

        // BAD: combobox missing aria-expanded should be flagged
        assertTrue(
            "BAD combobox should trigger missing aria-expanded",
            messages(badViolations).stream().anyMatch(m -> m.toLowerCase().contains("combobox") && m.toLowerCase().contains("aria-expanded"))
        );
    }

    @Test
    public void testRoleImgComposite_fromCookbook() throws Exception {
        Cookbook cb = loadCookbook();
        Example ex = getExample(cb, "role-img--composite");

        var okViolations = analyze("role-img-ok.html", ex.htmlOk);
        var badViolations = analyze("role-img-bad.html", ex.htmlBad);

        // OK: wrapper role=img with aria-label should be fine
        assertFalse(
            "OK role=img wrapper should not be redundant",
            messages(okViolations).stream().anyMatch(m -> m.contains("Redundant role='img'"))
        );

        // BAD: role='img' on <img> is redundant (accept either engine wording)
        assertTrue(
            "BAD img role redundancy should be flagged",
            messages(badViolations).stream().anyMatch(m -> m.toLowerCase().contains("redundant role") && m.toLowerCase().contains("img"))
        );
    }

    @Test
    public void testRoleHeadingAriaLevel_fromCookbook() throws Exception {
        Cookbook cb = loadCookbook();
        Example ex = getExample(cb, "role-heading--aria-level");

        var okViolations = analyze("role-heading-ok.html", ex.htmlOk);
        var badViolations = analyze("role-heading-bad.html", ex.htmlBad);

        // OK: non-semantic element with role=heading and aria-level should be accepted
        assertFalse(
            "OK role=heading on non-heading element should not be redundant",
            messages(okViolations).stream().anyMatch(m -> m.toLowerCase().contains("redundant role"))
        );

        // BAD: role='heading' on <h2> is redundant; accept either of our engines' wordings
        assertTrue(
            "BAD role=heading on <h2> should be flagged as redundant",
            messages(badViolations).stream().anyMatch(m -> m.toLowerCase().contains("redundant role") && m.toLowerCase().contains("heading"))
        );
    }

    @Test
    public void testRoleHeadingAriaLevel_fluid_fromCookbook() throws Exception {
        Cookbook cb = loadCookbook();
        Example ex = getExample(cb, "role-heading--aria-level");

        var okViolations = analyze("role-heading-ok-fluid.html", ex.fluidOk);
        var badViolations = analyze("role-heading-bad-fluid.html", ex.fluidBad);

        assertFalse(
            "OK Fluid role=heading on non-heading element should not be redundant",
            messages(okViolations).stream().anyMatch(m -> m.toLowerCase().contains("redundant role"))
        );

        assertTrue(
            "BAD Fluid role=heading on <h2> should be flagged as redundant",
            messages(badViolations).stream().anyMatch(m -> m.toLowerCase().contains("redundant role") && m.toLowerCase().contains("heading"))
        );
    }

    @Test
    public void testRegionLabelRequirement_fromCookbook() throws Exception {
        Cookbook cb = loadCookbook();
        Example ex = getExample(cb, "aria-labelledby--region");

        var okViolations = analyze("region-ok.html", ex.htmlOk);

        // OK: role=region requires labeling via aria-label or aria-labelledby; present here
        assertFalse(
            "OK region labeling should not be reported missing",
            messages(okViolations).stream().anyMatch(m -> m.contains("Role 'region' requires"))
        );
    }
}
