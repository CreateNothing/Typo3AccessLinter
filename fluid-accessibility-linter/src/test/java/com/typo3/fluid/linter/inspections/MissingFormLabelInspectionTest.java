package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class MissingFormLabelInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenTextInputMissingLabel() {
        String html = "<input type=\"text\">";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "text field missing label for accessibility");
    }

    @Test
    public void testShouldNotWarn_whenSubmitButtonHasValue() {
        String html = "<input type=\"submit\" value=\"Save\">";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "missing label for accessibility");
    }

    @Test
    public void testShouldWarn_whenTextareaMissingLabel() {
        String html = "<textarea ></textarea>";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Textarea missing label for accessibility");
    }

    @Test
    public void testShouldWarn_whenSelectMissingLabel() {
        String html = "<select ><option>A</option></select>";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Select element missing label for accessibility");
    }

    @Test
    public void testShouldNotWarn_whenImplicitLabelWrapsInput() {
        String html = "<label for=\"first\">First</label><input id=\"first\">";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "missing label for accessibility");
    }

    @Test
    public void testShouldWarn_whenFluidFormViewHelperMissingLabel() {
        String html = "<f:form.textfield />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'textfield' missing label");
    }
}
