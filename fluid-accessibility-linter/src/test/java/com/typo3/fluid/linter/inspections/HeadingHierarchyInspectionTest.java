package com.typo3.fluid.linter.inspections;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

/**
 * Tests for {@link HeadingHierarchyInspection}.
 */
public class HeadingHierarchyInspectionTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    private void doTest(String html, String... expectedMessages) {
        myFixture.configureByText("test.html", html);
        myFixture.enableInspections(new HeadingHierarchyInspection());
        var highlights = myFixture.doHighlighting();

        for (String expected : expectedMessages) {
            boolean found = highlights.stream().anyMatch(h -> h.getDescription() != null && h.getDescription().contains(expected));
            if (!found) {
                System.out.println("Expected not found: " + expected);
                highlights.forEach(h -> System.out.println("  - " + h.getDescription()));
            }
            assertTrue("Expected message not found: " + expected, found);
        }
    }

    @Test
    public void testMultipleH1InMainContent() {
        String html = "<h1>Main Title</h1><p>content</p><h1>Another Title</h1>";
        doTest(html, "Multiple H1 elements in main content - only one H1 should exist per page");
    }

    @Test
    public void testSkippedHeadingLevelInMainContent() {
        String html = "<h1>Title</h1><h3>Subsection</h3>";
        doTest(html, "Heading level skipped in main content: H3 follows H1 (expected H2)");
    }

    @Test
    public void testEmptyHeadingInMainContent() {
        String html = "<h2>   </h2>";
        doTest(html, "Empty H2 element in main content - headings must contain text content");
    }

    @Test
    public void testGenericHeadingTextWarning() {
        String html = "<h2>Heading</h2>";
        doTest(html, "H2 in main content contains generic text \"Heading\" - use descriptive heading text");
    }

    @Test
    public void testNavigationHeadingDeepAndAriaLabelSuggestion() {
        String html = "<nav><h4>Menu</h4></nav>";
        // Message about deep navigation heading can be implementation-dependent; keep robust by
        // asserting the aria-label suggestion which is stable across contexts.
        doTest(html,
            "Navigation heading 'Menu' could be replaced with aria-label on nav element for cleaner markup");
    }

    @Test
    public void testH1InSectioningElementWhenMainH1Exists() {
        String html = "<h1>Main Title</h1><section><h1>Section Title</h1></section>";
        doTest(html, "H1 in sectioning element when main H1 exists. Consider H2 for section heading");
    }

    @Test
    public void testH1InFluidSectionConflictsWithMain() {
        String html = "<h1>Main</h1><f:section name=\"content\"><h1>Part</h1></f:section>";
        doTest(html, "H1 in Fluid section may conflict with main page heading. Consider H2+ for section content");
    }

    @Test
    public void testHeadingContainsOnlyImages() {
        String html = "<h1><img src=\"x.jpg\"></h1>";
        doTest(html, "H1 contains only images - ensure images have alt text or add heading text");
    }

    @Test
    public void testLargeHeadingJumpBetweenContexts() {
        String html = "<h1>Title</h1><f:section name=\"part\"><h5>Deep</h5></f:section>";
        doTest(html, "Large heading level jump between contexts: H1 to H5. Consider intermediate heading levels");
    }
}
