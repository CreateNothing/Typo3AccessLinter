package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class RadioGroupFieldsetInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldError_whenRadioGroupNotInFieldset() {
        String html = "<input type=\"radio\" name=\"opt\"> <input type=\"radio\" name=\"opt\">";
        var highlights = highlight(html, new RadioGroupFieldsetInspection());
        assertHighlightsContain(highlights, "Radio button group 'opt' should be wrapped in <fieldset> with <legend>");
    }

    @Test
    public void testShouldWarn_whenCheckboxGroupNotInFieldset() {
        String html = "<input type=\"checkbox\"> <input type=\"checkbox\"> <input type=\"checkbox\">";
        var highlights = highlight(html, new RadioGroupFieldsetInspection());
        assertHighlightsContain(highlights, "Related checkboxes should be grouped in <fieldset> with <legend>");
    }

    @Test
    public void testShouldError_whenFieldsetMissingLegend() {
        String html = "<fieldset><input type=\"radio\" name=\"a\"></fieldset>";
        var highlights = highlight(html, new RadioGroupFieldsetInspection());
        assertHighlightsContain(highlights, "Fieldset missing <legend> element");
    }

    @Test
    public void testShouldError_whenLegendIsEmpty() {
        String html = "<fieldset><legend> </legend><input type=\"radio\" name=\"a\"></fieldset>";
        var highlights = highlight(html, new RadioGroupFieldsetInspection());
        assertHighlightsContain(highlights, "Legend element is empty");
    }
}
