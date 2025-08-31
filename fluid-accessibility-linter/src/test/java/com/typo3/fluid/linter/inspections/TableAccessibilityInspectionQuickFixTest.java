package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;
import org.junit.Test;

import java.util.List;

public class TableAccessibilityInspectionQuickFixTest extends BaseInspectionTest {

    @Test
    public void testConvertFirstRowToHeaders() {
        String html = wrapBody("<table class=\"data\"><tr><td>A</td><td>B</td></tr><tr><td>1</td><td>2</td></tr></table>");
        highlight("test.html", html, new TableAccessibilityInspection());
        applyFixContaining("Convert first row to header cells");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("<th scope=\"col\">A</th>"));
        assertTrue(updated.contains("<th scope=\"col\">B</th>"));
    }

    @Test
    public void testAddCaptionForComplexTable() {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < 6; i++) rows.append("<tr><td>a</td><td>b</td><td>c</td><td>d</td><td>e</td></tr>");
        String html = wrapBody("<table>" + rows + "</table>");
        highlight("test.html", html, new TableAccessibilityInspection());
        applyFixContaining("Add table caption");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("<caption>Table</caption>"));
    }

    @Test
    public void testAddTableSections() {
        StringBuilder rows = new StringBuilder();
        rows.append("<tr><th>H1</th><th>H2</th></tr>");
        for (int i = 0; i < 12; i++) rows.append("<tr><td>a</td><td>b</td></tr>");
        String html = wrapBody("<table>" + rows + "</table>");
        highlight("test.html", html, new TableAccessibilityInspection());
        applyFixContaining("Add table sections (thead/tbody)");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("<thead>"));
        assertTrue(updated.contains("</thead><tbody>"));
        assertTrue(updated.contains("</tbody>"));
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
