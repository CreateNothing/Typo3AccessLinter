package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.junit.Test;

import java.util.List;

public class NavigationLandmarkInspectionQuickFixTest extends BaseInspectionTest {

    @Test
    public void testShouldOfferFix_whenNavHasRedundantRole() {
        String html = wrapBody("<nav role='navigation'><a href='#'>Home</a></nav>");

        var highlights = highlight("test.html", html, new NavigationLandmarkInspection());
        List<String> fixes = quickFixTexts();

        assertFixesContain(fixes, "Remove redundant role attribute");
    }

    @Test
    public void testShouldAddAriaLabel_whenMultipleNavs() {
        String html = wrapFull("""
            <header>Head</header>
            <nav aria-label='Primary'><a href='#'>A</a></nav>
            <nav><a href='#'>B</a></nav>
            <footer>Foot</footer>
            """);

        // Enable inspection and collect fixes
        highlight("test.html", html, new NavigationLandmarkInspection());
        // Apply: Add aria-label to navigation (for the second unlabeled <nav>)
        applyFixContaining("Add aria-label to navigation");

        String updated = myFixture.getFile().getText();
        // Expect aria-label added to the second <nav>
        assertTrue("Expected aria-label to be added to unlabeled <nav>",
                updated.contains("<nav aria-label=\"Navigation\">") ||
                updated.contains("<nav aria-label=\"Navigation\" "));
    }

    @Test
    public void testShouldInsertMain_whenMissing() {
        StringBuilder body = new StringBuilder("<header>Header</header><nav>Menu</nav><div>");
        while (body.length() < 600) body.append("content ");
        body.append("</div>");
        String html = wrapFull(body.toString());

        highlight("test.html", html, new NavigationLandmarkInspection());
        applyFixContaining("Add <main> landmark for primary content");

        String updated = myFixture.getFile().getText();
        int bodyIdx = updated.toLowerCase().indexOf("<body>");
        int mainIdx = updated.toLowerCase().indexOf("<main role=\"main\">");
        assertTrue("Expected <main role=\"main\"> to be inserted after <body>",
                mainIdx > -1 && (bodyIdx == -1 || mainIdx > bodyIdx));
    }

    @Test
    public void testShouldRemoveRedundantRole_onNavRole() {
        String html = wrapBody("<nav role=\"navigation\">Menu</nav>");

        highlight("test.html", html, new NavigationLandmarkInspection());
        applyFixContaining("Remove redundant role attribute");

        String updated = myFixture.getFile().getText();
        assertFalse("Expected redundant role attribute to be removed",
                updated.contains(" role=\"navigation\"") || updated.contains(" role='navigation'"));
    }

    private void applyFixContaining(String needle) {
        List<IntentionAction> fixes = myFixture.getAllQuickFixes();
        IntentionAction target = fixes.stream()
                .filter(a -> a.getText() != null && a.getText().contains(needle))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Quick-fix not found containing: " + needle +
                        "\nAvailable: " + fixes.stream().map(IntentionAction::getText).toList()));
        myFixture.launchAction(target);
    }
}
