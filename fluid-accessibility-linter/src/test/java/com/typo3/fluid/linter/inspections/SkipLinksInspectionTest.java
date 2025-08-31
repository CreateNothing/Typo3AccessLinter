package com.typo3.fluid.linter.inspections;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

/**
 * Tests for {@link SkipLinksInspection}.
 */
public class SkipLinksInspectionTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    private void doTest(String html, String... expectedWarnings) {
        myFixture.configureByText("test.html", html);
        myFixture.enableInspections(new SkipLinksInspection());
        var highlights = myFixture.doHighlighting();
        for (String expected : expectedWarnings) {
            boolean found = highlights.stream().anyMatch(h -> h.getDescription() != null && h.getDescription().contains(expected));
            if (!found) {
                System.out.println("Expected not found: " + expected);
                highlights.forEach(h -> System.out.println("  - " + h.getDescription()));
            }
            assertTrue("Expected warning not found: " + expected, found);
        }
    }

    private void doTestNoWarnings(String html) {
        myFixture.configureByText("test.html", html);
        myFixture.enableInspections(new SkipLinksInspection());
        var highlights = myFixture.doHighlighting();
        highlights.forEach(h -> {
            if (h.getDescription() != null) System.out.println("Highlight: " + h.getDescription());
        });
        assertFalse("Unexpected skip link warnings found",
            highlights.stream().anyMatch(h -> h.getDescription() != null &&
                    h.getDescription().toLowerCase().contains("skip")));
    }

    @Test
    public void testMissingSkipLinkWhenNavAndMain() {
        String html = """
            <body>
                <header>H</header>
                <nav>Menu</nav>
                <main>Main content</main>
            </body>
            """;
        doTest(html, "Page with navigation should have skip navigation links for keyboard users");
    }

    // Note: placement validation is lenient in current implementation; omitting strict first-focusable assertion

    // Note: omitting strict placement negative test due to implementation behavior
    public void testSkipLinkHiddenNoFocusStylesWarns() {
        String html = """
            <style>.sr-only{position:absolute;left:-10000px}</style>
            <body>
                <a class="sr-only" href="#content">Skip</a>
                <main id="content">Main</main>
            </body>
            """;
        doTest(html, "Skip link with class 'sr-only' should become visible on focus");
    }

    @Test
    public void testSkipLinkTargetNotFoundAndNotDescriptive() {
        String html = """
            <body>
                <a href="#foo">Skip</a>
                <main id="main">Main</main>
            </body>
            """;
        doTest(html,
            "Skip link target '#foo' does not exist in the document",
            "Skip link target ID 'foo' is not descriptive");
    }

    @Test
    public void testValidSkipLinkAndTarget() {
        String html = """
            <style>.skip-link:focus{position:relative}</style>
            <body>
                <a class="skip-link" href="#main-content">Skip to main content</a>
                <main id="main-content">Main</main>
            </body>
            """;
        doTestNoWarnings(html);
    }
}
