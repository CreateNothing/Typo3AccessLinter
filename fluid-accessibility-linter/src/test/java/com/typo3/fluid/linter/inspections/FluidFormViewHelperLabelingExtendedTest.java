package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class FluidFormViewHelperLabelingExtendedTest extends BaseInspectionTest {

    @Test
    public void testShouldWarn_whenFluidTextfieldMissingLabelOrProperty() {
        String html = "<f:form.textfield />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertHighlightsContain(highlights, "Fluid form ViewHelper 'textfield' missing label");
    }

    @Test
    public void testShouldNotWarn_whenFluidTextfieldUsesProperty() {
        // property is treated as an indicator of framework-provided labeling
        String html = "<f:form.textfield property=\"email\" />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "missing label");
    }

    @Test
    public void testShouldNotWarn_whenLabelForMatchesFluidId() {
        String html = """
            <label for=\"username\">Username</label>
            <f:form.textfield id=\"username\" />
            """;
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "missing label");
    }

    @Test
    public void testShouldNotWarn_forHiddenAndSubmitViewHelpers() {
        String html = "<f:form.hidden name=\"token\" value=\"abc\" />\n<f:form.submit value=\"Save\" />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "Fluid form ViewHelper");
    }

    @Test
    public void testShouldNotWarn_whenAriaLabelProvidedOnFluidControl() {
        String html = "<f:form.textfield aria-label=\"Search\" />";
        var highlights = highlight(html, new MissingFormLabelInspection());
        assertNoHighlightsContaining(highlights, "missing label");
    }
}
