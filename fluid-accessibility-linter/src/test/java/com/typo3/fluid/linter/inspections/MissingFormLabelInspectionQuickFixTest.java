package com.typo3.fluid.linter.inspections;

import com.intellij.codeInsight.intention.IntentionAction;

import java.util.List;

public class MissingFormLabelInspectionQuickFixTest extends BaseInspectionTest {

    public void testPlaceholderToLabel_fixAddsLabelAndId() {
        String html = wrapBody("<input type=\"text\" placeholder=\"Your full name please\">\n");
        highlight("test.html", html, new MissingFormLabelInspection());
        applyFixContaining("Add label and use placeholder for hint");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("<label for=\"fld-"));
        assertTrue(updated.contains(">Your full name please</label>"));
        assertTrue(updated.contains(" id=\"fld-"));
    }

    public void testRequiredIndicator_fixAddsRequiredAndAria() {
        String html = wrapBody("<input type=\"text\" placeholder=\"Email address (required)\">\n");
        highlight("test.html", html, new MissingFormLabelInspection());
        applyFixContaining("Move required indication to proper attributes");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("required"));
        assertTrue(updated.contains("aria-required=\"true\""));
    }

    public void testAddLegendToFieldset_whenMultipleInputsNoLegend() {
        String html = wrapBody("<fieldset>\n  <input type=\"text\">\n  <input type=\"text\">\n</fieldset>");
        highlight("test.html", html, new MissingFormLabelInspection());
        applyFixContaining("Add legend to fieldset");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.toLowerCase().contains("<legend>group</legend>"));
    }

    public void testGroupDateTimeFields_wrapsContainerWithFieldset() {
        String body = "<div class=\"date-group\">\n" +
                "  <input name=\"date_day\" type=\"text\">\n" +
                "  <input name=\"date_month\" type=\"text\">\n" +
                "</div>";
        String html = wrapBody(body);
        highlight("test.html", html, new MissingFormLabelInspection());
        applyFixContaining("Group related fields in fieldset");
        String updated = myFixture.getFile().getText();
        assertTrue(updated.contains("<fieldset>"));
        assertTrue(updated.contains("</fieldset>"));
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
