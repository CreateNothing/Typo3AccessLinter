package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class FluidFormControlsAccessibilityTest extends BaseInspectionTest {

    // textfield already covered elsewhere; include a couple for completeness of matrix

    @Test
    public void testShouldWarn_whenPasswordMissingLabel() {
        String html = "<f:form.password />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'password' missing label");
    }

    @Test
    public void testShouldNotWarn_whenPasswordHasAriaLabel() {
        String html = "<f:form.password aria-label=\"Account password\" />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "missing label");
    }

    @Test
    public void testShouldWarn_whenTextareaMissingLabel() {
        String html = "<f:form.textarea />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'textarea' missing label");
    }

    @Test
    public void testShouldWarn_whenSelectMissingLabel() {
        String html = "<f:form.select><f:form.select.option value=\"a\"/></f:form.select>";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'select' missing label");
    }

    @Test
    public void testShouldNotWarn_whenSelectLabelAssociated() {
        String html = """
            <label for=\"country\">Country</label>
            <f:form.select id=\"country\">
                <f:form.select.optgroup label=\"Europe\">
                    <f:form.select.option value=\"DE\" />
                </f:form.select.optgroup>
            </f:form.select>
            """;
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "missing label");
    }

    @Test
    public void testShouldWarn_whenCheckboxMissingLabel() {
        String html = "<f:form.checkbox />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'checkbox' missing label");
    }

    @Test
    public void testShouldWarn_whenRadioMissingLabel() {
        String html = "<f:form.radio />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'radio' missing label");
    }

    @Test
    public void testShouldWarn_whenCountrySelectMissingLabel() {
        String html = "<f:form.countrySelect />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'countrySelect' missing label");
    }

    @Test
    public void testShouldNotWarn_whenSubmitAndHiddenAreExempt() {
        String html = "<f:form.hidden name=\"token\" value=\"abc\" />\n<f:form.submit value=\"Save\" />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "Fluid form ViewHelper");
    }

    @Test
    public void testShouldWarn_whenUploadMissingLabel() {
        String html = "<f:form.upload />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'upload' missing label");
    }

    @Test
    public void testShouldWarn_whenUploadDeleteCheckboxMissingLabel() {
        String html = "<f:form.uploadDeleteCheckbox />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'uploadDeleteCheckbox' missing label");
    }

    @Test
    public void testShouldNotWarn_whenButtonHasVisibleText() {
        String html = "<f:form.button>Save</f:form.button>";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "missing label");
    }
}

