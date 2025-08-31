package com.typo3.fluid.linter.inspections;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

/**
 * Tests for {@link NavigationLandmarkInspection}.
 */
public class NavigationLandmarkInspectionTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    private void doTest(String html, String... expectedWarnings) {
        myFixture.configureByText("test.html", html);
        myFixture.enableInspections(new NavigationLandmarkInspection());
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
        myFixture.enableInspections(new NavigationLandmarkInspection());
        var highlights = myFixture.doHighlighting();
        assertFalse("Unexpected navigation warnings found",
            highlights.stream()
                .anyMatch(h -> h.getDescription() != null &&
                        h.getDescription().toLowerCase().matches(".*(navigation|landmark|main|nav).*")));
    }

    @Test
    public void testMultipleNavsRequireLabels() {
        String html = """
            <header>Head</header>
            <nav><a href="/a">A</a></nav>
            <nav><a href="/b">B</a></nav>
            <main>Main</main>
            """;
        doTest(html, "Give each navigation area a unique label");
    }

    @Test
    public void testDuplicateNavLabels() {
        String html = """
            <nav aria-label="Main navigation"><a href="/a">A</a></nav>
            <nav aria-label="Main navigation"><a href="/b">B</a></nav>
            """;
        doTest(html, "Duplicate navigation label 'Main navigation'");
    }

    @Test
    public void testRedundantRoleOnNav() {
        String html = "<nav role=\"navigation\">Menu</nav>";
        doTest(html, "Redundant role='navigation' on <nav> element");
    }

    @Test
    public void testMissingMainOnLargePage() {
        StringBuilder sb = new StringBuilder("<header>Header</header><nav>Menu</nav><div>");
        while (sb.length() < 600) sb.append("content ");
        sb.append("</div>");
        doTest(sb.toString(), "Add a <main> landmark for primary content");
    }

    @Test
    public void testMultipleMainLandmarks() {
        String html = "<main>One</main><div>content</div><main>Two</main>";
        doTest(html, "Use only one <main> landmark per page");
    }

    @Test
    public void testNavListStructureRequiredWhenManyLinks() {
        String html = """
            <nav>
                <a href="/a">A</a>
                <a href="/b">B</a>
                <a href="/c">C</a>
                <a href="/d">D</a>
            </nav>
            """;
        doTest(html, "Wrap multiple navigation links in a list");
    }

    @Test
    public void testNavWithFluidLinksCountsToo() {
        String html = """
            <nav>
                <f:link.action action="a">A</f:link.action>
                <f:link.action action="b">B</f:link.action>
                <f:link.action action="c">C</f:link.action>
                <f:link.action action="d">D</f:link.action>
            </nav>
            """;
        doTest(html, "Wrap multiple navigation links in a list");
    }

    @Test
    public void testValidLabeledNavWithList() {
        String html = """
            <header>H</header>
            <nav aria-label="Main navigation">
                <ul>
                    <li><a href="/">Home</a></li>
                    <li><f:link.page pageUid="1">About</f:link.page></li>
                </ul>
            </nav>
            <main>Main</main>
            <footer>F</footer>
            """;
        doTestNoWarnings(html);
    }
}
